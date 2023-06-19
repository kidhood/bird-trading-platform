package com.gangoffive.birdtradingplatform.service.impl;

import com.gangoffive.birdtradingplatform.api.response.ApiResponse;
import com.gangoffive.birdtradingplatform.api.response.ErrorResponse;
import com.gangoffive.birdtradingplatform.dto.AccountUpdateDto;
import com.gangoffive.birdtradingplatform.entity.Account;
import com.gangoffive.birdtradingplatform.entity.Address;
import com.gangoffive.birdtradingplatform.entity.Channel;
import com.gangoffive.birdtradingplatform.entity.ShopOwner;
import com.gangoffive.birdtradingplatform.enums.AccountStatus;
import com.gangoffive.birdtradingplatform.exception.CustomRuntimeException;
import com.gangoffive.birdtradingplatform.mapper.AccountMapper;
import com.gangoffive.birdtradingplatform.repository.AccountRepository;
import com.gangoffive.birdtradingplatform.repository.AddressRepository;
import com.gangoffive.birdtradingplatform.repository.VerifyTokenRepository;
import com.gangoffive.birdtradingplatform.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AddressRepository addressRepository;
    private final VerifyTokenRepository verifyTokenRepository;

    @Override
    public Account updateAccount(AccountUpdateDto accountUpdateDto) {
//        log.info("acc {}", accountUpdateDto.toString());
        Optional<Account> editAccount = accountRepository.findByEmail(accountUpdateDto.getEmail());
        editAccount.get().setFullName(accountUpdateDto.getFullName());
        editAccount.get().setPhoneNumber(accountUpdateDto.getPhoneNumber());
        if (editAccount.get().getAddress() == null) {
            Address address = new Address();
            address.setPhone(accountUpdateDto.getPhoneNumber());
            address.setStreet(accountUpdateDto.getStreet());
            address.setWard(accountUpdateDto.getWard());
            address.setDistrict(accountUpdateDto.getDistrict());
            address.setCity(accountUpdateDto.getCity());
            addressRepository.save(address);
            editAccount.get().setAddress(address);
        } else {
            Address addressUpdate = editAccount.get().getAddress();
            addressUpdate.setPhone(accountUpdateDto.getPhoneNumber());
            addressUpdate.setStreet(accountUpdateDto.getStreet());
            addressUpdate.setWard(accountUpdateDto.getWard());
            addressUpdate.setDistrict(accountUpdateDto.getDistrict());
            addressUpdate.setCity(accountUpdateDto.getCity());
            addressRepository.save(addressUpdate);
        }
        return accountRepository.save(editAccount.get());
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyToken(String token, boolean isResetPassword) {
        log.info("token {}", token);
        var tokenRepo = verifyTokenRepository.findByToken(token);
        if (tokenRepo.isPresent()) {
            if (!tokenRepo.get().isRevoked()) {
                Date expireDate = tokenRepo.get().getExpired();
                Date timeNow = new Date();
                if (timeNow.after(expireDate)) {
                    ErrorResponse errorResponse = new ErrorResponse().builder().errorCode(HttpStatus.BAD_REQUEST.toString())
                            .errorMessage("This link has already expired. Please regenerate the link to continue the verification").build();
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
                if (!isResetPassword) {
                    var account = tokenRepo.get().getAccount();
                    account.setStatus(AccountStatus.VERIFY);
                    accountRepository.save(account);
                }
                tokenRepo.get().setRevoked(true);
                verifyTokenRepository.save(tokenRepo.get());

                return ResponseEntity.ok(new ApiResponse(LocalDateTime.now(), "Verification of the account was successful!"));
            }
            ErrorResponse errorResponse = new ErrorResponse().builder().errorCode(HttpStatus.BAD_REQUEST.toString())
                    .errorMessage("This verify link has already used!").build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        ErrorResponse errorResponse = new ErrorResponse().builder().errorCode(HttpStatus.NOT_FOUND.toString())
                .errorMessage("Not found token. Link not true").build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    public long retrieveShopID(long accountId) {
        var acc = accountRepository.findById(accountId);
        if(acc.isPresent()) {
            ShopOwner shopOwner = acc.get().getShopOwner();
            if(shopOwner != null) {
                return shopOwner.getId();
            }else {
                throw new CustomRuntimeException("400", String.format("Cannot found shop with account id: %d",accountId));
            }
        } else {
            throw new CustomRuntimeException("400", String.format("Cannot found account with account id: %d",accountId));
        }
    }

    @Override
    @Transactional
    public List<Long> getAllChanelByUserId(long userId) {
        var acc = accountRepository.findById(userId);
        if(acc.isPresent()) {
            List<Channel> channels = acc.get().getChannels();
            if( channels != null || channels.size() != 0) {
                List<Long> listShopId = channels.stream().map(channel -> channel.getShopOwner().getId()).toList();
                return listShopId;
            } else {
                throw new CustomRuntimeException("400", "Cannot find channel");
            }
        } else {
            throw new CustomRuntimeException("400", String.format("Cannot find account with id %d", userId));
        }
    }
}
