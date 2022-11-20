package br.com.juliancambraia.aws_project02.service;

import br.com.juliancambraia.aws_project02.model.Envelope;
import br.com.juliancambraia.aws_project02.model.ProductEvent;
import br.com.juliancambraia.aws_project02.model.ProductEventLog;
import br.com.juliancambraia.aws_project02.model.SnsMessage;
import br.com.juliancambraia.aws_project02.repository.ProductEventLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;

@Service
public class ProductEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ObjectMapper mapper;

    private final ProductEventLogRepository productEventLogRepository;

    @Autowired
    public ProductEventConsumer(ObjectMapper objectMapper, ProductEventLogRepository productEventLogRepository) {
        this.mapper = objectMapper;
        this.productEventLogRepository = productEventLogRepository;
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

        var productEventLog = buildProductEventLog(envelope, productEvent);

        productEventLogRepository.save(productEventLog);
    }

    private ProductEventLog buildProductEventLog(Envelope envelope, ProductEvent productEvent) {

        long timestamp = Instant.now().toEpochMilli();

        ProductEventLog productEventLog = new ProductEventLog();

        productEventLog.setPk(productEvent.getCode());
        productEventLog.setSk(envelope.getEventType() + "_" + timestamp);
        productEventLog.setEventType(envelope.getEventType());
        productEventLog.setProductId(productEvent.getProductId());
        productEventLog.setUsername(productEvent.getUsername());
        productEventLog.setTimestamp(timestamp);

        // Criando uma Data no futuro mais 10 minutos com o Objetivo de que o DynamoDB exclua os Itens acima criados a
        // partir de 10 minutos.
        productEventLog.setTtl(Instant.now().plus(
                Duration.ofMillis(10)).getEpochSecond());

        return productEventLog;

    }
}
