# Order Matching Engine

Simulated exchange order matching engine with price-time priority and concurrent order processing.

## Features
- Price-time priority matching (orders matched by price, then timestamp)
- Thread-safe order book using ConcurrentSkipListMap and ReentrantReadWriteLock
- REST API for order submission and order book queries
- Event publishing to AWS SNS for order lifecycle tracking
- Handles partial fills across multiple price levels

## Quick Start
```bash
mvn spring-boot:run
# Application runs on localhost:8080

# Create order book
curl -X POST localhost:8080/api/orderbook \
  -H "Content-Type: application/json" \
  -d '{"symbol":"AAPL"}'

# Submit order
curl -X POST localhost:8080/api/orderbook/AAPL \
  -H "Content-Type: application/json" \
  -d '{"side":"BUY","quantity":100,"price":150.50,"symbol":"AAPL"}'

# View order book
curl localhost:8080/api/orderbook/AAPL
```

## Architecture
- **OrderBook**: Maintains bid/ask levels using ConcurrentSkipListMap for sorted, thread-safe price level access
- **OrderBookLevel**: FIFO queue of orders at same price, protected by ReentrantReadWriteLock
- **Matching Engine**: Walks through price levels to execute trades, handles partial fills
- **Event Publishing**: Publishes ORDER_CREATED, ORDER_CANCELLED, and TRADE_EXECUTED events to AWS SNS

## Technical Details
**Data Structures:**
- ConcurrentSkipListMap for O(log n) sorted price level operations
- ConcurrentHashMap for O(1) order lookup by ID
- LinkedList for FIFO order queues within each price level

**Complexity:**
- Order insertion: O(log n) for new price level, O(1) for existing level
- Order matching: O(k) where k = number of price levels to walk through
- Best bid/ask: O(log n)

**Testing:**
- Comprehensive unit tests for controllers, services, and entities
- Integration tests with Testcontainers and LocalStack for SNS
- Concurrency tests with 1000+ simultaneous threads validating thread safety

## Technology Stack
Java 17 | Spring Boot 3.4 | AWS SNS | Maven | Docker | Testcontainers

## Run Tests
```bash
mvn test
```

## Future Improvements
- Market orders (currently limit orders only)
- Custom LinkedList implementation for O(1) order removal (currently O(n))
- WebSocket streaming for real-time order book updates

## Author
Built to understand exchange mechanics and concurrent programming patterns.