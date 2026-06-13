package hello.itemservice.domain;

import lombok.Data;

import javax.persistence.*;

@Data
// @Entity: 이 애노테이션이 있어야 JPA가 인식할 수 있음
@Entity
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", length = 10)
    private String itemName;
    private Integer price;
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
