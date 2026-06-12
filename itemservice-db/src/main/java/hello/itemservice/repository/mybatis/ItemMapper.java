package hello.itemservice.repository.mybatis;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/*
    해당 클래스는 마이바티스 매핑 XML을 호출해주는 매퍼 인터페이스이다.
    이 인터페이스에는 @Mapper 애노테이션을 붙여줘야지 MyBatis에서 인식할 수 있다.
    이 인터페이스의 메서드를 호출하면 xml의 해당 SQL을 실행하고 결과를 돌려준다.
*/

/*
    인터페이스인데 어떻게 빈으로 등록이 되는 거지?
        1. 애플리케이션 로딩 시점에 MyBatis 스프링 연동 모듈은 '@Mapper'가 붙어있는 인터페이스를 조사
        2. 해당 인터페이스가 발견되면 동적 프록시 기술을 사용해서 'ItemMapper' 인터페이스의 구현체를 만듦
        3. 생성된 구현체를 스프링 빈으로 등록

    매퍼 구현체
        마이바티스 스프링 연동 모듈이 만들어주는 'ItemMapper'의 구현체 덕분에 인터페이스 만으로 편리하게 XML의 데이터를 찾아서 호출할 수 있다.
        원래 마이바티스를 사용하려면 더 번잡한 코드를 거쳐야 하는데, 이런 부분을 인터페이스 하나로 매우 깔끔하고 편리하게 사용할 수 있다.
        매퍼 구현체는 MyBatis에서 발생한 예외를 스프링 예외 추상화인 DataAccessException에 맞게 변환해서 반환해준다.
*/
@Mapper
public interface ItemMapper {

    void save(Item item);

    void update(@Param("id") Long id, @Param("updateParam") ItemUpdateDto updateParam);

    Optional<Item> findById(Long id);

    List<Item> findAll(ItemSearchCond itemSearch);
}
