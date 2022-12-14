package br.com.juliancambraia.aws_project01.service;

import br.com.juliancambraia.aws_project01.enums.EventType;
import br.com.juliancambraia.aws_project01.model.Envelope;
import br.com.juliancambraia.aws_project01.model.Product;
import br.com.juliancambraia.aws_project01.model.ProductEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProductPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPublisher.class);

    private final AmazonSNS snsClient;
    private final Topic productEventsTopic;
    private final ObjectMapper mapper;

    @Autowired
    public ProductPublisher(AmazonSNS snsClient,
                            @Qualifier("productEventsTopic") Topic productEventsTopic,
                            ObjectMapper objectMapper) {

        this.snsClient = snsClient;
        this.productEventsTopic = productEventsTopic;
        this.mapper = objectMapper;
    }

    public void publishProductEvent(Product product, EventType eventType, String username) {
        ProductEvent productEvent = new ProductEvent();
        productEvent.setProductId(product.getId());
        productEvent.setCode(product.getCode());
        productEvent.setUsername(username);

        Envelope envelope = new Envelope();
        envelope.setEventType(eventType);

        try {
            envelope.setData(mapper.writeValueAsString(productEvent));

            var uuidMessage = snsClient.publish(
                    productEventsTopic.getTopicArn(),
                    mapper.writeValueAsString(envelope));
            LOG.info("Product Event Sent - Event: {} - ProductId: {} - Identificador único capturada do Pub: {} ",
                    envelope.getEventType(),
                    productEvent.getProductId(),
                    uuidMessage.getMessageId());
        } catch (JsonProcessingException e) {
            LOG.error("Failed to create product event message");
        }
    }
}
