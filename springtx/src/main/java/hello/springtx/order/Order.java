package hello.springtx.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
// 대부분 DB에서 order는 order by의 예약어로 많이 사용되고 있어 order이라는 단어를 테이블명으로 사용할 수가 없음
    // 그래서 orders라고 테이블을 이름을 지정해서 많이 사용함
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private String username; // 정상, 예외, 잔고부족
    private String payStatus; // 대기, 완료

}
