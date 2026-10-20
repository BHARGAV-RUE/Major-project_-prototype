# ===================================================================
# CloudTransfer Core (Brain) - Application Configuration
# ===================================================================

# Server Port
server.port=${SERVER_PORT_CORE:8080}

# Application Name
spring.application.name=cloudtransfer-core

# PostgreSQL Database Connection
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/cloudtransfer}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:123}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA & Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Polygon Amoy Blockchain Settings
blockchain.rpc.url=${BLOCKCHAIN_RPC_URL:https://polygon-amoy.g.alchemy.com/v2/dummy_url_for_now}
blockchain.wallet.private.key=${BLOCKCHAIN_WALLET_PRIVATE_KEY:0000000000000000000000000000000000000000000000000000000000000000}
blockchain.contract.address=${BLOCKCHAIN_CONTRACT_ADDRESS:0x0000000000000000000000000000000000000000}

# RabbitMQ Connection Settings (CloudAMQP)
spring.rabbitmq.addresses=${SPRING_RABBITMQ_ADDRESSES:amqps://zvjkottu:Zo-FoVsGw12ihn9t2y9URsJLSZ4Uc2RV@gerbil.rmq.cloudamqp.com/zvjkottu}

# Google OAuth Credentials (for Contributor Registration)
google.oauth.client-id=${GOOGLE_OAUTH_CLIENT_ID:1044051799903-eik3tnt93k7bnrp9e18qkr5ulm8ke4r1.apps.googleusercontent.com}
google.oauth.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET: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}
google.oauth.redirect-uri=${GOOGLE_OAUTH_REDIRECT_URI:http://localhost:8080/api/v1/contributors/oauth/google/callback}

# Dropbox OAuth Credentials (for Contributor Registration)
dropbox.oauth.client-id=${DROPBOX_OAUTH_CLIENT_ID:grxaen3vrsmbvs5}
dropbox.oauth.client-secret=${DROPBOX_OAUTH_CLIENT_SECRET:5kelkfc477kxl6g}
dropbox.oauth.redirect-uri=${DROPBOX_OAUTH_REDIRECT_URI:http://localhost:8080/api/v1/contributors/oauth/dropbox/callback}