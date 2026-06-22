package hello.springtx.apply;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class InitTxTest {

    @Autowired Hello hello;

    @Test
    void go () {
        // 초기화 코드는 스프링이 초기화 시점에 호출한다.
//        hello.intiV1(); 이렇게 직접 호출하게 되면 트랜잭션 호출이 됨
            // 왜냐하면 직접 호출한다는 것은 이미 객체를 생성을 다 하고 초기화 메서드 또한 실행된 이후의 시점이므로
                // 이때에는 트랜잭션 AOP가 적용되기 때문에 트랜잭션이 호출될 수 있는 것

    }

    @TestConfiguration
    static class InitTxTestConfig {
        @Bean
        Hello hello() {
            return new Hello();
        }
    }

    @Slf4j
    static class Hello {

        // @PostConstruct: 스프링 빈이 생성되고 의존성 주입이 모두 완료된 직후 자동으로 실행되는 메서드
            // 객체 생성 -> 의존성 주입 -> @PostConstruct 실행 순서로 동작이 됨
        /*
            초기화 코드(예: @PostConstruct)와 @Transactional을 함께 사용하면 트랜잭션이 적용되지 않음
            초기화 코드가 먼저 호출되고, 그 다음에 트랜잭션 AOP가 적용되기 때문이다.
                따라서 초기화 시점에는 해당 메서드에서 트랜잭션을 획득할 수 없다.
        */
        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init @PostConstruct tx active={} ", isActive);
        }

        /*
            initV1의 방법으로는 초기화시점에 트랜잭션을 획득해서 초기화 메서드를 실행하고 싶은데 AOP의 생성시점이 초기화 시점보다
            늦으므로 트랜잭션을 획득해서 실행하지 못하므로 초기화 메서드에서 트랜잭션을 획득한 뒤 실행하도록 하고 싶으면
            아래와 같은 방법을 대안으로 사용할 수 있다.
        */
        // @EventListener(ApplicationReadyEvent.class): 스프링 부트 애플리케이션이 완전히 시작(Ready)된 후 해당 애노테이션이 선언된 메서드를 실행하라는 의미
        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init ApplicationReadyEvent tx active={} ", isActive);
        }

    }

}
