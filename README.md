# Exchange 

Simulated exchange, using a distrubted systems approach to facilitate high throughput and low latency trading.

## Features
### Functional
- Place orders on symbol
- Match orders by price-time priority
- Get status of order
- Get aggregated view of orderbook
- Serve real-time updates on state of orderbook

### Non-functional (aims)
- Low latency orders (<2ms p90)
- High throuput orders (20k/s on each symbol)
- Strong consistency of orders and trades
- Eventual consistency on order status
- High throughput order reads
- High throughput orderbook reads/updates
- Low latency orderbook reads/updates 

## Components
- **Matching Engine**: Servers order requests using in-memory orderbook. Publishes order/trade events to Kafka (I plan to add a disk WAL to write events to instead of publishing straight to Kafka, decreasing latency). 
- **Order Persist Service**: Consumes Kafka events and writes to database to reflect new state.
- **Order Query Service**: Serves order read requests by querying database. **Why?** decreases load on matching engine significantly, allowing for lower latency and higher throughput order requests.
- **Market Data Service**: Serves orderbook state requests, and orderbook subscriptions (through websocket). Maintains orderbook state in-memory by consuming Kafka events.
- **API gateway**
- **Kafka**
- **Order Entry Service**: (WIP) Orchestrates order write requests. Handles user authorisation and locks user balance. This isn't scoped for the current implementation, but is worth a mention. 

<img width="5174" height="2288" alt="image" src="https://github.com/user-attachments/assets/5ea64ba9-7b91-45f0-9e9c-4bea6924831f" />



## Quick Start
```bash
mvn install
docker-compose up -d
mvn spring-boot:run

# Create order
curl -X POST localhost:8000/api/orders \
  -H "Content-Type: application/json" \
  -d '{"side":"BUY","quantity":100,"price":150.50,"symbol":"AAPL"}'

# View order - replace {orderId} with the orderId recieved in body of POST response
curl http://localhost:8000/api/orders/{orderId}
```
