package fastcampus.team7.Livable_officener.service;

import fastcampus.team7.Livable_officener.domain.Building;
import fastcampus.team7.Livable_officener.domain.Company;
import fastcampus.team7.Livable_officener.domain.User;
import fastcampus.team7.Livable_officener.dto.*;
import fastcampus.team7.Livable_officener.dto.BuildingWithCompaniesDTO;
import fastcampus.team7.Livable_officener.dto.LoginDTO.LoginRequestDTO;
import fastcampus.team7.Livable_officener.dto.LoginDTO.LoginResponseDTO;
import fastcampus.team7.Livable_officener.global.exception.*;
import fastcampus.team7.Livable_officener.global.sercurity.JwtProvider;
import fastcampus.team7.Livable_officener.global.util.RedisUtil;
import fastcampus.team7.Livable_officener.repository.BuildingRepository;
import fastcampus.team7.Livable_officener.repository.CompanyRepository;
import fastcampus.team7.Livable_officener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static fastcampus.team7.Livable_officener.dto.BuildingWithCompaniesDTO.*;
import static fastcampus.team7.Livable_officener.dto.BuildingWithCompaniesDTO.BuildingWithCompaniesResponseDTO.CompanyResponseDTO;
import static fastcampus.team7.Livable_officener.dto.PhoneAuthDTO.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SignUpService {

    private final BuildingRepository buildingRepository;
    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;

    private final JwtProvider jwtProvider;

    private final RedisUtil redisUtil;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public BuildingWithCompaniesDTO getBuildingWithCompanies(String keyword) {

        List<Building> buildings = buildingRepository.findBuildingsByNameContaining(keyword);
        List<BuildingWithCompaniesResponseDTO> buildingWithCompaniesDTOList = new ArrayList<>();
        BuildingWithCompaniesDTO response = new BuildingWithCompaniesDTO();

        if (buildings.isEmpty()) {
            response.setBuildings(List.of());
            return response;
        }

        for (Building building : buildings) {
            List<CompanyResponseDTO> officeDTOs = getOfficeDTOs(building);

            buildingWithCompaniesDTOList.add(
                    BuildingWithCompaniesResponseDTO.builder()
                            .id(building.getId())
                            .buildingName(building.getName())
                            .buildingAddress(building.getAddress())
                            .offices(officeDTOs)
                            .build()
            );
        }

        response.setBuildings(buildingWithCompaniesDTOList);

        return response;
    }

    public PhoneAuthResponseDTO getPhoneAuthCode(PhoneAuthRequestDTO request) {

        String requestPhoneNumber = request.getPhoneNumber();

        if (userRepository.existsByPhoneNumber(requestPhoneNumber)) {
            throw new DuplicatedPhoneNumberException();
        }

        String phoneAuthCode = redisUtil.getPhoneAuthCode(requestPhoneNumber);

        if (ObjectUtils.isEmpty(phoneAuthCode)) {

            return PhoneAuthResponseDTO.builder()
                    .verifyCode(redisUtil.setPhoneAuthCode(requestPhoneNumber))
                    .build();
        }

        return PhoneAuthResponseDTO.builder()
                .verifyCode(redisUtil.changePhoneAuthCode(requestPhoneNumber))
                .build();

    }

    @Transactional(readOnly = true)
    public boolean confirmVerifyCode(PhoneAuthConfirmDTO request) {

        String requestPhoneNumber = request.getPhoneNumber();
        String requestVerifyCode = request.getVerifyCode();

        if (!redisUtil.hasPhoneAuthCode(requestPhoneNumber)) {
            throw new NotVerifiedPhoneNumberException();
        }

        if (!redisUtil.getPhoneAuthCode(requestPhoneNumber).equals(requestVerifyCode)) {
            throw new NotVerifiedPhoneAuthCodeException();
        }

        return true;

    }

    public void signUp(SignUpRequestDTO request) {

        Building building = buildingRepository.findByName(request.getBuildingName())
                .orElseThrow(() -> new NotFoundBuildingException());

        Company company = companyRepository.findByName(request.getCompanyName())
                .orElseThrow(() -> new NotFoundCompanyException());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicatedUserEmailException();
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicatedPhoneNumberException();
        }

        User user = request.toEntity(building, company, passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

    }

    public LoginDTO login(LoginRequestDTO request) {

        String requestEmail = request.getEmail();
        String requestPassword = request.getPassword();

        User user = userRepository.findByEmail(requestEmail)
                .orElseThrow(() -> new NotFoundUserException());

        if (!passwordEncoder.matches(requestPassword, user.getPassword())) {
            throw new InvalidPasswordException();
        }

        BuildingResponseDTO buildingDTO = BuildingResponseDTO.builder()
                .id(user.getBuilding().getId())
                .buildingName(user.getBuilding().getName())
                .buildingAddress(user.getBuilding().getAddress())
                .build();

        CompanyResponseDTO companyDTO = CompanyResponseDTO.builder()
                .id(user.getCompany().getId())
                .officeName(user.getCompany().getName())
                .officeNum(user.getCompany().getAddress())
                .build();

        String token = jwtProvider.createToken(requestEmail);

        LoginResponseDTO responseBody = LoginResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .building(buildingDTO)
                .office(companyDTO)
                .token(token)
                .build();

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUserInfo(responseBody);

        return loginDTO;
    }

    public void logout(User user, String authorization) {

        String bearerTokenPrefix = jwtProvider.getBearerTokenPrefix(authorization);
        Long expirationTime = jwtProvider.getExpirationTime(bearerTokenPrefix);

        redisUtil.setBlackList(bearerTokenPrefix, user.getEmail(), expirationTime);

    }


    private List<CompanyResponseDTO> getOfficeDTOs(Building building) {

        List<Company> companies = companyRepository.findCompaniesByBuildingName(building.getName());
        List<CompanyResponseDTO> officeDTOs = new ArrayList<>();

        for (Company company : companies) {

            CompanyResponseDTO officeDTO = CompanyResponseDTO.builder()
                    .id(company.getId())
                    .officeName(company.getName())
                    .officeNum(company.getAddress())
                    .build();

            officeDTOs.add(officeDTO);
        }

        return officeDTOs;

    }
}
