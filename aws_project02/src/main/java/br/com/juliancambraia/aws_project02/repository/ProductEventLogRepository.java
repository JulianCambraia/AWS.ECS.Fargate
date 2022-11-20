package br.com.juliancambraia.aws_project02.repository;

import br.com.juliancambraia.aws_project02.model.ProductEventKey;
import br.com.juliancambraia.aws_project02.model.ProductEventLog;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
public interface ProductEventLogRepository extends CrudRepository<ProductEventLog, ProductEventKey> {

    List<ProductEventLog> findAllByPk(String code);

    List<ProductEventLog> findAllByPkAndSkStartingWith(String code, String eventType);
}
