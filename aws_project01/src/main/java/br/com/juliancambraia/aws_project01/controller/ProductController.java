package br.com.juliancambraia.aws_project01.controller;

import br.com.juliancambraia.aws_project01.enums.EventType;
import br.com.juliancambraia.aws_project01.model.Product;
import br.com.juliancambraia.aws_project01.repository.ProductRepository;
import br.com.juliancambraia.aws_project01.service.ProductPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("api/products")
public class ProductController {

    private ProductRepository repository;
    private ProductPublisher productPublisher;


    @Autowired
    public ProductController(ProductRepository repository, ProductPublisher productPublisher) {
        this.repository = repository;
        this.productPublisher = productPublisher;
    }

    @GetMapping
    public Iterable<Product> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable("id") long id) {
        Optional<Product> optionalProduct = repository.findById(id);

        return optionalProduct.map(product -> new ResponseEntity<>(product, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        var productCreated = repository.save(product);

        productPublisher.publishProductEvent(productCreated, EventType.PRODUCT_CREATED, "mariaines");

        return new ResponseEntity<>(productCreated, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@RequestBody Product product, @PathVariable("id") long id) {
        if (repository.existsById(id)) {
            product.setId(id);

            var productUpdated = repository.save(product);

            productPublisher.publishProductEvent(productUpdated, EventType.PRODUCT_UPDATE, "marcelocardoso");

            return new ResponseEntity<>(productUpdated, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Product> delete(@PathVariable("id") long id) {
        Optional<Product> optionalProduct = repository.findById(id);

        if (optionalProduct.isPresent()) {
            var product = optionalProduct.get();
            repository.delete(product);

            productPublisher.publishProductEvent(product, EventType.PRODUCT_DELETED, "geraldo");

            return new ResponseEntity<>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/byCode")
    public ResponseEntity<Product> findByCode(@RequestParam String code) {
        Optional<Product> optionalProduct = repository.findByCode(code);
        return optionalProduct.map(
                        product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
