package fastcampus.team7.Livable_officener.repository;

import fastcampus.team7.Livable_officener.dto.signup.PhoneAuthDTO;
import org.springframework.data.repository.CrudRepository;

public interface PhoneAuthDTORedisRepository extends CrudRepository<PhoneAuthDTO, String> {
}
