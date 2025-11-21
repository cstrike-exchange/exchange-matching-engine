package org.louisjohns32.personal.exchange.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.louisjohns32.personal.exchange.constants.Side;
import org.louisjohns32.personal.exchange.entities.Order;
import org.louisjohns32.personal.exchange.services.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

//@Disabled("Requires Docker")
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SNSOrderEventsIntegrationTests {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
        .withServices(SNS, SQS); 

    static SnsClient snsClient;
    static String topicArn;
    
    static {
    	localstack.start();
    }
    
    @Autowired
    private OrderBookService orderBookService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("spring.cloud.aws.sns.endpoint", () -> localstack.getEndpointOverride(SNS).toString());
        
        String region = localstack.getRegion();
        String accountId = "000000000000"; 
        String topicName = "exchange-order-created";
        String topicArn = String.format("arn:aws:sns:%s:%s:%s", region, accountId, topicName);
        registry.add("sns_topic_arn", () -> topicArn);
    }

    @BeforeAll
    public void setUp() {
        snsClient = SnsClient.builder()
            .endpointOverride(localstack.getEndpointOverride(SNS)) 
            .region(Region.of(localstack.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                )
            )
            .build();

        
        CreateTopicResponse createTopicResponse = snsClient.createTopic(CreateTopicRequest.builder()
            .name("exchange-order-created")
            .build());
        topicArn = createTopicResponse.topicArn();
    }
    
    private static String generateSQSPolicy(String queueArn, String topicArn) {
        return "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": { \"Service\": \"sns.amazonaws.com\" },\n" +
            "      \"Action\": \"sqs:SendMessage\",\n" +
            "      \"Resource\": \"" + queueArn + "\",\n" +
            "      \"Condition\": {\n" +
            "        \"ArnEquals\": { \"aws:SourceArn\": \"" + topicArn + "\" }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    @Test
    void whenOrderCreated_thenSnsEventIsPublished() throws InterruptedException, JsonMappingException, JsonProcessingException {
    	// sqs queue for checking published sns messages
        SqsClient sqsClient = SqsClient.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
            .region(Region.of(localstack.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                )
            )
            .build();

        String queueName = "test-queue";
        CreateQueueResponse createQueueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
            .queueName(queueName)
            .build());
        String queueUrl = createQueueResponse.queueUrl();
        
        // get sqs arn after set up
        String queueArn = null;

        for (int i = 0; i < 5; i++) {
            GetQueueAttributesResponse attrResp = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build());

            queueArn = attrResp.attributes().get(QueueAttributeName.QUEUE_ARN);
            System.out.println("Attempt " + (i + 1) + ": queueArn = " + queueArn);

            if (queueArn != null && !queueArn.isBlank()) {
                break;
            }

            Thread.sleep(1000); 
        }

        if (queueArn == null || queueArn.isBlank()) {
            throw new IllegalStateException("QueueArn not available after retries");
        }

        // set queue policy
        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributes(Map.of(
                QueueAttributeName.POLICY, generateSQSPolicy(queueArn, topicArn)
            ))
            .build());

        // subscribe sqs to sns
        SubscribeResponse subResp = snsClient.subscribe(SubscribeRequest.builder()
            .topicArn(topicArn)
            .protocol("sqs")
            .endpoint(queueArn)
            .attributes(Map.of("RawMessageDelivery", "true"))
            .build());


        Order order = new Order(Side.BUY, 1.0, 100.0);
        orderBookService.createOrderBook("BTCUSD");
        orderBookService.createOrder("BTCUSD", order);
        

        // poll SQS for the message (with retries)
        String messageBody = null;
        for (int i = 0; i < 5; i++) {
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(2)
                .build()).messages();

            if (!messages.isEmpty()) {
                messageBody = messages.get(0).body();
                break;
            }

            Thread.sleep(1000);
        }
        
  

        assertNotNull(messageBody, "Expected to receive SNS event in SQS queue");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(messageBody);
        assertEquals("BTCUSD", json.get("symbol").asText());
        assertEquals(order.getQuantity(), json.get("quantity").asDouble());
        assertEquals(order.getPrice(), json.get("price").asDouble());
        assertEquals("ORDER_CREATED", json.get("eventType").asText());
    }
    
    
}
