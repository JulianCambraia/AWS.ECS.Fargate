package br.com.juliancambraia.aws_project02.service;

import br.com.juliancambraia.aws_project02.model.Envelope;
import br.com.juliancambraia.aws_project02.model.ProductEvent;
import br.com.juliancambraia.aws_project02.model.SnsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Service
public class ProductEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ObjectMapper mapper;

    @Autowired
    public ProductEventConsumer(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    @JmsListener(destination = "${aws.sqs.queue.product.events.name}")
    public void receiveProductEvent(TextMessage textMessage) throws JMSException, JsonProcessingException {
        SnsMessage snsMessage = mapper.readValue(textMessage.getText(), SnsMessage.class);

        Envelope envelope = mapper.readValue(snsMessage.getMessage(), Envelope.class);

        ProductEvent productEvent = mapper.readValue(envelope.getData(), ProductEvent.class);

        LOG.info("Product event received - Event: {} -  ProductId: {} - MessageId: {}",
                envelope.getEventType(),
                productEvent.getProductId(),
                snsMessage.getMessageId());

    }
}
