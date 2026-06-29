package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired LogRepository logRepository;

    /*
        memberService    @Transactional:OFF
        memberRepository @Transactional:ON
        logRepository    @Transactional:ON
    */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        // when
        memberService.joinV1(username);

        // when: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /*
        memberService    @Transactional:OFF
        memberRepository @Transactional:ON
        logRepository    @Transactional:ON Exception
    */
    @Test
    void outerTxOff_fail() {
        // given
        String username = "로그예외_outerTxOff_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        // when: 로그 데이터는 저장 시 런타임 에러가 발생되어 롤백된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /*
        memberService    @Transactional:ON
        memberRepository @Transactional:OFF
        logRepository    @Transactional:OFF
    */
    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        // when
        memberService.joinV1(username);

        // when: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /*
        memberService    @Transactional:ON
        memberRepository @Transactional:ON
        logRepository    @Transactional:ON
    */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        // when
        memberService.joinV1(username);

        // when: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /*
       memberService    @Transactional:ON
       memberRepository @Transactional:ON
       logRepository    @Transactional:ON Exception
    */
    /*
        LogRepository 로직에서 런타임 예외가 발생되면 트랜잭션 AOP가 예외를 받게 되고 런타임 예외가 발생했으므로 트랜잭션 매니저에게 롤백을 요청
            이 경우 LogRepository가 가진 트랜잭션은 신규 트랜잭션이 아니므로 물리 롤백을 호출하지는 않고 대신에 rollbackOnly를 설정한다.
        LogRepository가 예외를 잡아서 처리하는 것이 아니라 던졌기 때문에 트랜잭션 AOP도 해당 예외를 그대로 밖으로 던진다.
        그래서 MemberService에서도 런타임 예외를 받게 되고 런타임 예외를 처리하지 않고 밖으로 던지게 되므로 트랜잭션 AOP는 런타임 예외가 발생했으므로
        트랜잭션 매니저에 롤백을 요청하게 된다. 그럼 이 경우에는 신규 트랜잭션을 MemberService가 가지고 있으므로 물리 롤백을 호출하게 된다.
            참고로 이 경우 어차피 물리 트랜잭션이 롤백되었기 떄문에, rollbackOnly 설정은 참고하지 않는다.
            MemberService가 LogRepository에서 넘어온 런타임 예외를 던졌기 때문에 트랜잭션 AOP도 해당 예외를 그대로 밖으로 던진다.
            해당 로직을 호출한 클라이언트는 LogRepository부터 넘어온 런타임 예외를 받게 된다.
    */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        // when: 모든 데이터가 롤백된다.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
    
    /*
        memberService    @Transactional:ON
        memberRepository @Transactional:ON
        logRepository    @Transactional:ON Exception
    */
    /*
        아래의 코드는 joinV2() 메서드 내부의 로직에는 LogRepository에서 예외가 발생하면 런타임 예외를 잡아서 처리를 하므로 joinV2() 메서드에는
        런타임 예외가 던져지지 않으므로 물리 트랜잭션을 가지고 있는 joinV2()가 정상적으로 커밋이 되어 회원에 대한 정보는 데이터베이스에 저장이 될 것이라고
        예상하기 쉽다.
        하지만, 내부 트랜잭션에 롤백이 발생하면 rollbackOnly를 설정하기 때문에 결과적으로 정상 흐름 처리를 해서 외부 트랜잭션에서 커밋 호출을 해도
        물리 트랜잭션은 롤백이 되어 버리고 'UnexpectedRollbackException이 던져진다.
    */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        // when: 모든 데이터가 롤백된다.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
}