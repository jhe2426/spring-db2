package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
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

    
}
