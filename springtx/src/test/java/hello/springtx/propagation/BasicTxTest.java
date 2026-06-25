package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    @Test
    void duble_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);

    }

    @Test
    void duble_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);

    }


    /*
        같은 메서드에서 맨 처음 트랜잭션을 얻어서 해당 작업을 하고 있는데, 이 트랜잭션의 반납이 이루어지지 않은 상태(커밋을 하지 않은)에서
        트랜잭션 매니져에게 트랜잭션을 가져와서 작업을 하게 되면 어떻게 될까?
            이때 맨 처음에 가져온 트랜잭션을 외부 트랜잭션이라고 하며, 그 다음에 가져온 트랜잭션을 내부 트랜잭션이라고한다.
            이렇게 하나의 메서드에서 2개의 트랜잭션이 존재하기 되면, 스프링에서는 그 각 각의 트랜잭션을 논리 트랜잭션이라고 하고
            이 논리 트랜잭션을 감싼 물리 트랜잭션이 존재하다. 이 논리 트랜잭션이 커밋을 하든 롤백을 해도 실제 물리 트랜잭션은
            항상 이 논리 트랜잭션들이 전부 다 커밋을 해야지만 실제 DB에 커밋을 반영하고, 하나라도 롤백의 작업이 일어난다면 물리 트랜잭션또한
            롤백의 작업이 일어나도록 설계되어 있다.

       내부 트랜잭션을 시작할 때 'Participating in existing transaction'이라는 메시지를 로그에서 확인할 수 있는데,
       이 메시지는 내부 트랜잭션이 기존에 존재하는 외부 트랜잭션에 참여한다는 뜻이다.
       실행 로그를 보면 외부 트랜잭션을 시작하거나 커밋할 때는 DB 커넥션을 통한 물리 트랜잭션을 시작하고, DB 커넥션을 통해 커밋된다.
       그런데 내부 트랜잭션을 시작하거나 커밋할 때는 DB 커넥션을 통해 커밋하는 로그를 전혀 확인할 수 없다.
       따라서 외부 트랜잭션만 물리 트랜잭션을 시작하고, 커밋한다.
       만약 내부 트랜잭션이 실제 물리 트랜잭션을 커밋하게 되면 트랜잭션이 끝나버리기 때문에, 트랜잭션을 처음으로 시작한 외부 트랜잭션까지 실행 결과를 이어갈 수
       없기 때문에 내부 트랜잭션은 DB 커넥션을 통한 물리 트랜잭션을 커밋하면 안 된다.
       * 스프링은 이렇게 여러 트랜잭션이 함께 사용된는 경우, "처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리"하도록 하여 트랜잭션 중복
            커밋 문제를 해결한다.
    */
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }


    /*
        내부 트랜잭션을 롤백하면 'Participating transaction failed - marking existing transaction as rollback-only'으로
        실제 물리 트랜잭션이 커밋되지 못하도록 롤백만할 수 있도록 스프링에서 해당 물리 트랜잭션에게 마크를 한다.
        그래서 내부 트랜잭션을 롤백하면 실제 물리 트랜잭션은 롤백하지 않는다. 그리고 기존 트랜잭션인 외부 트랜잭션을 롤백 전용으로 표시한다.

        내부 트랜잭션을 롤백한 뒤 외부 트랜잭션을 커밋하게 되면 'Global transaction is marked as rollback-only'으로 전체 트랜잭션이
        롤백 전용으로 표시되어 있다는 것을 로그로 확인할 수 있다. 따라서 물리 트랜잭션을 롤백하게 된다.

        외부 트랜잭션에서는 커밋을 호출한 개발자 입장에서는 커밋을 기대했는데 내부 트랜잭션이 롤백을 하는 바람에 롤백 전용 표시로 인해 실제로는 롤백이
        일어난 이 상황에서 아무 메시지도 없이 조용히 넘어가도 되는 문제가 아니므로 시스템 입장에서는 커밋을 호출했지만 롤백이 되었다는 것을 분명히 알려줘야 함
        아무 메시지를 전달해주지 않게 되면 고객은 주문이 성공했다고 생각했는데, 실제로는 롤백이 되어서 주문이 데이터베이스에 저장되지 않은 상황이 발생이 일어날 수 있는 것
        그래서 스프링은 이 경우 'UnexpectedRollbackException' 런타임 예외를 던져서 커밋을 시도했지만, 기대하지 않은 롤백이 발생했다는 것을 명확하게 알려줌
    */
    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);

        log.info("외부 트랜잭션 커밋");
        Assertions.assertThatThrownBy(() -> txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    /*
        외부 트랜잭션 시작
        - 외부 트랜잭션을 시작하면서 'conn0'를 획득하고 'manual commit'으로 변경해서 물리 트랜잭션을 시작
        - 외부 트랜잭션은 신규 트랜잭션이다.('outer.isNewTransaction()=true')

        내부 트랜잭션 시작
        - 내부 트랜잭션을 시작하면서 'conn1'를 획득하고 'manual commit'으로 변경해서 물리 트랜잭션을 시작한다.
        - 내부 트랜잭션은 외부 트랜잭션에 참여하는 것이 아니라, 'PROPAGATION_REQUIRES_NEW' 옵션을 사용했기 때문에 완전히 새로운 신규 트랜잭션으로
            생성된다.('inner.isNewTransaction()=true')

        내부 트랜잭션 롤백
        - 내부 트랜잭션을 롤백하면 내부 트랜잭션은 신규 트랜잭션이므로 실제 물리 트랜잭션을 롤백한다.
        - 내부 트랜잭션은 'conn1'을 사용하므로 'conn1'에 물리 롤백을 수행한다.

        외부 트랜잭션 커밋
        - 외부 트랜잭션을 커밋하면 외부 트랜잭션은 신규 트랜잭션이기 때문에 실제 물리 트랜잭션을 커밋한다.
        - 외부 트랜잭션은 'conn0'를 사용하므로 'conn0'에 물리 커밋을 수행한다.


        요청 흐름 - 내부 트랜잭션
        - 트랜잭션 매니저는 'REQUIRES_NEW' 옵션을 확인하면 기존 트랜잭션이 존재하면 이 기존 트랜잭션에 참여하는 것이 아니라 새로운 트랜잭션을 시작해준다.
        - 트랜잭션 매니저는 데이터소스를 통해 커넥션을 생성한다.
        - 생성한 커넥션을 수동 커밋 모드('setAutoCommit(false)')로 설정한다. -> 물리 트랜잭션 시작
        - 트랜잭션 매니저는 트랜잭션 동기화 매니저에 커넥션을 보관한다.
            - 이때 'con1'은 잠시 보류되고, 지금부터는 'con2'가 사용된다. (내부 트랜잭션을 완료할 때까지 'con2'가 사용된다.)
        - 트랜잭션 매니저는 신규 트랜잭션의 생성한 결과를 반환한다. 'isNewTransaction == true'
        - 내부 트랜잭션을 사용하는 로직2에서 커넥션이 필요한 경우 트랜잭션 동기화 매니저에 있는 'con2' 커넥션을 획득해서 사용한다.


       응답 흐름 - 내부 트랜잭션
       - 로직2가 끝나고 트랜잭션 매니저를 통해 내부 트랜잭션을 롤백한다.
       - 트랜잭션 매니저는 롤백 시점에 신규 트랜잭션 여부에 따라 다르게 동작한다. 현재 내부 트랜잭션은 신규 트랜잭션이므로 실제 롤백을 호출한다.
       - 내부 트랜잭션이 'con2' 물리 트랜잭션을 롤백한다.
            - 트랜잭션이 종료되고, 'con2'는 종료되거나, 커넥션 풀에 반납된다.
            - 이후에 'con1'의 보류가 끝나고, 다시 'con1'을 사용한다.
    */
    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction()); // true

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); // true

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner); // 롤백

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer); // 커밋
    }

}
