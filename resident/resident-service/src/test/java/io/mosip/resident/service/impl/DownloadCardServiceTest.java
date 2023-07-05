package io.mosip.resident.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.resident.constant.PacketStatus;
import io.mosip.resident.constant.ResidentConstants;
import io.mosip.resident.constant.ResidentErrorCode;
import io.mosip.resident.constant.TransactionStage;
import io.mosip.resident.dto.CheckStatusResponseDTO;
import io.mosip.resident.dto.DigitalCardStatusResponseDto;
import io.mosip.resident.dto.DownloadCardRequestDTO;
import io.mosip.resident.dto.DownloadPersonalizedCardDto;
import io.mosip.resident.dto.IdentityDTO;
import io.mosip.resident.dto.MainRequestDTO;
import io.mosip.resident.dto.ResidentCredentialResponseDto;
import io.mosip.resident.dto.ResponseWrapper;
import io.mosip.resident.dto.VidDownloadCardResponseDto;
import io.mosip.resident.entity.ResidentTransactionEntity;
import io.mosip.resident.exception.ApisResourceAccessException;
import io.mosip.resident.exception.OtpValidationFailedException;
import io.mosip.resident.exception.ResidentCredentialServiceException;
import io.mosip.resident.exception.ResidentServiceCheckedException;
import io.mosip.resident.exception.ResidentServiceException;
import io.mosip.resident.repository.ResidentTransactionRepository;
import io.mosip.resident.service.DownloadCardService;
import io.mosip.resident.service.IdAuthService;
import io.mosip.resident.service.NotificationService;
import io.mosip.resident.service.ResidentCredentialService;
import io.mosip.resident.service.ResidentVidService;
import io.mosip.resident.util.AuditUtil;
import io.mosip.resident.util.ResidentServiceRestClient;
import io.mosip.resident.util.Utilities;
import io.mosip.resident.util.Utility;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * This class is used to create service class test for getting cards.
 * 
 * @Author Kamesh Shekhar Prasad
 */
@RunWith(MockitoJUnitRunner.class)
@RefreshScope
@ContextConfiguration
public class DownloadCardServiceTest {

	private static final String LOCALE_EN_US = "en-US";

	@InjectMocks
	private DownloadCardService downloadCardService = new DownloadCardServiceImpl();

	@Mock
	private IdAuthService idAuthService;

	@Mock
	private Utilities utilities;

	@Mock
	private Environment environment;

	@Mock
	private ResidentServiceRestClient residentServiceRestClient;

	@Mock
	private AuditUtil auditUtil;

	@Mock
	private ResidentCredentialService residentCredentialService;

	@Mock
	private IdentityServiceImpl identityService;

	@Mock
	private NotificationService notificationService;

	@Mock
	private Utility utility;

	@Mock
	private ResidentTransactionRepository residentTransactionRepository;

	@Mock
	private ResidentVidService vidService;

	private MainRequestDTO<DownloadCardRequestDTO> downloadCardRequestDTOMainRequestDTO;

	private String result;

	byte[] pdfbytes;

	DigitalCardStatusResponseDto digitalCardStatusResponseDto;

	private MainRequestDTO<DownloadPersonalizedCardDto> downloadPersonalizedCardMainRequestDTO;

	private Map identityMap;

	private ResidentTransactionEntity residentTransactionEntity;

	@Before
	public void setup() throws Exception {
		downloadCardRequestDTOMainRequestDTO = new MainRequestDTO<>();
		DownloadCardRequestDTO downloadCardRequestDTO = new DownloadCardRequestDTO();
		downloadCardRequestDTO.setTransactionId("1234567890");
		downloadCardRequestDTO.setOtp("111111");
		downloadCardRequestDTO.setIndividualId("7841261580");
		downloadCardRequestDTOMainRequestDTO.setRequest(downloadCardRequestDTO);
		result = "result";
		pdfbytes = result.getBytes();
		Mockito.when(utility.signPdf(Mockito.any(), Mockito.any())).thenReturn(pdfbytes);
		Mockito.when(utilities.getRidByIndividualId(Mockito.anyString())).thenReturn("1234567890");
		Mockito.when(residentCredentialService.getCard(Mockito.anyString(), isNull(), isNull())).thenReturn(pdfbytes);
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("UIN");
		Mockito.when(identityService.getIndividualIdForAid(Mockito.anyString())).thenReturn("7841261580");
		Mockito.when(utility.createEntity(Mockito.any())).thenReturn(new ResidentTransactionEntity());
		Mockito.when(utility.createEventId()).thenReturn("12345");

		residentTransactionEntity = new ResidentTransactionEntity();
		residentTransactionEntity.setEventId("12345");
		downloadPersonalizedCardMainRequestDTO = new MainRequestDTO<>();
		DownloadPersonalizedCardDto downloadPersonalizedCardDto = new DownloadPersonalizedCardDto();
		downloadPersonalizedCardDto.setHtml(
				"PGh0bWw+PGhlYWQ+PC9oZWFkPjxib2R5Pjx0YWJsZT48dHI+PHRkPk5hbWU8L3RkPjx0ZD5GUjwvdGQ+PC90cj48dHI+PHRkPkRPQjwvdGQ+PHRkPjE5OTIvMDQvMTU8L3RkPjwvdHI+PHRyPjx0ZD5QaG9uZSBOdW1iZXI8L3RkPjx0ZD45ODc2NTQzMjEwPC90ZD48L3RyPjwvdGFibGU+PC9ib2R5PjwvaHRtbD4=");
		downloadPersonalizedCardDto.setAttributes(List.of("gender", "fullName"));
		downloadPersonalizedCardMainRequestDTO.setRequest(downloadPersonalizedCardDto);
		Mockito.when(environment.getProperty(ResidentConstants.MOSIP_CREDENTIAL_TYPE_PROPERTY))
				.thenReturn("credentialType");
		Mockito.when(environment.getProperty(ResidentConstants.CREDENTIAL_ISSUER)).thenReturn("credentialType");
		Mockito.when(environment.getProperty(ResidentConstants.CREDENTIAL_ENCRYPTION_FLAG)).thenReturn("true");
		Mockito.when(environment.getProperty(ResidentConstants.CREDENTIAL_ENCRYPTION_KEY)).thenReturn("true");
		Mockito.when(identityService.getResidentIndvidualIdFromSession()).thenReturn("1234567890");
		Mockito.when(identityService.getUinForIndividualId(Mockito.anyString())).thenReturn("3425636374");
		Mockito.when(utilities.getUinByVid(Mockito.anyString())).thenReturn("3425636374");
		Mockito.when(idAuthService.validateOtpV2(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.any())).thenReturn(Tuples.of(true, residentTransactionEntity));
		identityMap = new LinkedHashMap();
		identityMap.put("UIN", "8251649601");
		identityMap.put("email", "manojvsp12@gmail.com");
		identityMap.put("phone", "9395910872");
		identityMap.put("dateOfBirth", "1970");
	}

	@Test
	public void testGetDownloadCardPdfWithVID()
			throws ResidentServiceCheckedException, ApisResourceAccessException, IOException {
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put(ResidentConstants.AID_STATUS, PacketStatus.SUCCESS.getName());
		hashMap.put(ResidentConstants.TRANSACTION_TYPE_CODE, TransactionStage.CARD_READY_TO_DOWNLOAD.name());
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenReturn(hashMap);
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("VID");
		Tuple2<byte[], String> actualResult = downloadCardService
				.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
		assertNotNull(actualResult);
		assertEquals(pdfbytes, actualResult.getT1());
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetIndividualIdForAidWithResidentServiceCheckedException()
			throws ResidentServiceCheckedException, ApisResourceAccessException {
		Mockito.when(identityService.getIndividualIdForAid(Mockito.anyString()))
				.thenThrow(new ResidentServiceCheckedException());
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("AID");
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetIndividualIdForAidWithApisResourceAccessException()
			throws ResidentServiceCheckedException, ApisResourceAccessException {
		Mockito.when(identityService.getIndividualIdForAid(Mockito.anyString()))
				.thenThrow(new ApisResourceAccessException());
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("AID");
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetDownloadCardPdfWithValidateOTPFalse()
			throws ResidentServiceCheckedException, ApisResourceAccessException, OtpValidationFailedException {
		Mockito.when(idAuthService.validateOtpV2(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.any())).thenReturn(Tuples.of(false, residentTransactionEntity));
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetRidForIndividualIdWithApisResourceAccessException()
			throws OtpValidationFailedException, ApisResourceAccessException, ResidentServiceCheckedException {
		Mockito.when(idAuthService.validateOtpV2(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.any())).thenReturn(Tuples.of(true, residentTransactionEntity));
		Mockito.when(utilities.getRidByIndividualId(Mockito.anyString())).thenThrow(new ApisResourceAccessException());
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetDownloadCardPdfWithOtpValidationFailedException()
			throws OtpValidationFailedException, ApisResourceAccessException, ResidentServiceCheckedException {
		Mockito.when(idAuthService.validateOtpV2(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.any())).thenThrow(new OtpValidationFailedException());
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetDownloadCardPdfCardNotReady()
			throws ApisResourceAccessException, IOException, ResidentServiceCheckedException {
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put(ResidentConstants.AID_STATUS, PacketStatus.SUCCESS.getName());
		hashMap.put(ResidentConstants.TRANSACTION_TYPE_CODE, TransactionStage.UIN_GENERATION_STAGE.name());
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenReturn(hashMap);
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("AID");
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test
	public void testGetDownloadCardPdfZeroLength() throws Exception {
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put(ResidentConstants.AID_STATUS, PacketStatus.SUCCESS.getName());
		hashMap.put(ResidentConstants.TRANSACTION_TYPE_CODE, TransactionStage.CARD_READY_TO_DOWNLOAD.name());
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenReturn(hashMap);
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("AID");
		Mockito.when(residentCredentialService.getCard(Mockito.anyString(), isNull(), isNull()))
				.thenReturn(new byte[0]);
		Tuple2<byte[], String> actualResult = downloadCardService
				.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
		assertEquals(0, actualResult.getT1().length);
	}

	@Test(expected = ResidentServiceException.class)
	public void testGetDownloadCardPdfWithApisResourceAccessException() throws Exception {
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenThrow(new ApisResourceAccessException());
		Mockito.when(identityService.getIndividualIdType(Mockito.anyString())).thenReturn("AID");
		downloadCardService.getDownloadCardPDF(downloadCardRequestDTOMainRequestDTO);
	}

	@Test
	public void testDownloadPersonalizedCardWithAttributesListAndPassword()
			throws ResidentServiceCheckedException, IOException {
		Mockito.when(environment.getProperty(ResidentConstants.IS_PASSWORD_FLAG_ENABLED))
				.thenReturn(String.valueOf(true));
		Mockito.when(environment.getProperty(ResidentConstants.PASSWORD_ATTRIBUTE)).thenReturn("firstName|dateOfBirth");
		Map<String, Object> name = new HashMap<>();
		name.put("language", "eng");
		name.put("value", "kamesh");
		identityMap.put("firstName", List.of(name));
		Mockito.when(identityService.getIdentityAttributes(Mockito.anyString(), isNull())).thenReturn(identityMap);
		Mockito.when(utilities.getLanguageCode()).thenReturn("eng");
		Mockito.when(utility.getPassword(Mockito.anyList())).thenReturn("kame1970");
		Tuple2<byte[], String> actualResult = downloadCardService
				.downloadPersonalizedCard(downloadPersonalizedCardMainRequestDTO, 0, LOCALE_EN_US);
		assertEquals(pdfbytes, actualResult.getT1());
	}

	@Test(expected = ResidentServiceException.class)
	public void testDownloadPersonalizedCardPasswordFailed() throws ResidentServiceCheckedException {
		Mockito.when(environment.getProperty(ResidentConstants.IS_PASSWORD_FLAG_ENABLED))
				.thenReturn(String.valueOf(true));
		Mockito.when(utility.getPassword(Mockito.anyList()))
				.thenThrow(new ResidentServiceException(ResidentErrorCode.DOWNLOAD_PERSONALIZED_CARD));
		downloadCardService.downloadPersonalizedCard(downloadPersonalizedCardMainRequestDTO, 0, LOCALE_EN_US);
	}

	@Test(expected = ResidentServiceException.class)
	public void testDownloadPersonalizedCardResidentServiceCheckedException()
			throws ResidentServiceCheckedException, IOException {
		Mockito.when(identityService.getIdentityAttributes(Mockito.anyString(), Mockito.isNull()))
				.thenThrow(new ResidentServiceCheckedException());
		downloadCardService.downloadPersonalizedCard(downloadPersonalizedCardMainRequestDTO, 0, LOCALE_EN_US);
	}

	@Test(expected = ResidentServiceException.class)
	public void testDownloadPersonalizedCardIOException() throws ResidentServiceCheckedException, IOException {
		Mockito.when(identityService.getIdentityAttributes(Mockito.anyString(), Mockito.isNull()))
				.thenThrow(new IOException());
		downloadCardService.downloadPersonalizedCard(downloadPersonalizedCardMainRequestDTO, 0, LOCALE_EN_US);
	}

	@Test
	public void testGetVidCardEventIdWithVidDetails() throws BaseCheckedException, IOException {
		Mockito.when(identityService.getResidentIndvidualIdFromSession()).thenReturn("3257091426984315");
		ResponseWrapper<List<Map<String, ?>>> vidResponse = new ResponseWrapper<>();
		List<Map<String, ?>> vidList = new ArrayList<>();
		Map<String, Object> vidDetails = new HashMap<>();
		vidDetails.put("vidType", "perpetual");
		vidDetails.put("vid", "123");
		vidDetails.put("maskedVid", "1******4");
		vidDetails.put("expiryTimeStamp", "1234343434");
		vidDetails.put("expiryTimeStamp", "1234343434");
		vidDetails.put("genratedOnTimestamp", "1234343434");
		vidDetails.put("transactionLimit", "1234343434");
		vidDetails.put("transactionCount", "1234343434");
		vidList.add(vidDetails);
		vidResponse.setResponse(vidList);
		Mockito.when(vidService.retrieveVids(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(vidResponse);
		Map<String, Object> name = new HashMap<>();
		name.put("language", "eng");
		name.put("value", "kamesh");
		identityMap.put("firstName", List.of(name));
		Mockito.when(identityService.getIdentityAttributes(Mockito.anyString(), isNull())).thenReturn(identityMap);
		Mockito.when(environment.getProperty(ResidentConstants.PASSWORD_ATTRIBUTE)).thenReturn("firstName|dateOfBirth");
		Mockito.when(utilities.getLanguageCode()).thenReturn("eng");
		IdentityDTO identityDTO = new IdentityDTO();
		identityDTO.setDateOfBirth("1892-08-09");
		Mockito.when(identityService.getIdentity(Mockito.anyString())).thenReturn(identityDTO);
		ResponseWrapper<ResidentCredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		ResidentCredentialResponseDto residentCredentialResponseDto = new ResidentCredentialResponseDto();
		residentCredentialResponseDto.setId("123");
		residentCredentialResponseDto.setRequestId("123");
		responseWrapper.setResponse(residentCredentialResponseDto);
		Mockito.when(residentServiceRestClient.postApi(any(), any(), any(), any())).thenReturn(responseWrapper);
		Tuple2<ResponseWrapper<VidDownloadCardResponseDto>, String> tupleResponse = downloadCardService
				.getVidCardEventId("123", 0, LOCALE_EN_US);
		assertEquals("12345", tupleResponse.getT2());
		assertEquals(ResidentConstants.SUCCESS, tupleResponse.getT1().getResponse().getStatus());
	}

	@Test
	public void testGetVidCardEventIdWithNameNull() throws BaseCheckedException, IOException {
		Mockito.when(identityService.getResidentIndvidualIdFromSession()).thenReturn("3257091426984315");
		ResponseWrapper<List<Map<String, ?>>> vidResponse = new ResponseWrapper<>();
		List<Map<String, ?>> vidList = new ArrayList<>();
		Map<String, Object> vidDetails = new HashMap<>();
		vidDetails.put("vidType", "perpetual");
		vidDetails.put("vid", "123");
		vidDetails.put("maskedVid", "1******4");
		vidDetails.put("expiryTimeStamp", "1234343434");
		vidDetails.put("expiryTimeStamp", "1234343434");
		vidDetails.put("genratedOnTimestamp", "1234343434");
		vidDetails.put("transactionLimit", "1234343434");
		vidDetails.put("transactionCount", "1234343434");
		vidList.add(vidDetails);
		vidResponse.setResponse(vidList);
		Mockito.when(vidService.retrieveVids(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(vidResponse);
		IdentityDTO identityDTO = new IdentityDTO();
		identityDTO.setDateOfBirth("1892-08-09");
		Mockito.when(identityService.getIdentity(Mockito.anyString())).thenReturn(identityDTO);
		ResponseWrapper<ResidentCredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		ResidentCredentialResponseDto residentCredentialResponseDto = new ResidentCredentialResponseDto();
		residentCredentialResponseDto.setId("123");
		residentCredentialResponseDto.setRequestId("123");
		responseWrapper.setResponse(residentCredentialResponseDto);
		Mockito.when(residentServiceRestClient.postApi(any(), any(), any(), any())).thenReturn(responseWrapper);
		Tuple2<ResponseWrapper<VidDownloadCardResponseDto>, String> tupleResponse = downloadCardService
				.getVidCardEventId("123", 0, LOCALE_EN_US);
		assertEquals("12345", tupleResponse.getT2());
		assertEquals(ResidentConstants.SUCCESS, tupleResponse.getT1().getResponse().getStatus());
	}

	@Test(expected = ResidentServiceCheckedException.class)
	public void testGetVidCardEventIdNestedIf() throws BaseCheckedException, IOException {
		Mockito.when(utilities.getUinByVid(Mockito.anyString())).thenReturn("123456789");
		downloadCardService.getVidCardEventId("123", 0, LOCALE_EN_US);
	}

	@Test(expected = ResidentServiceCheckedException.class)
	public void testGetVidCardEventIdWithRequestCredentialFailed() throws BaseCheckedException {
		ResponseWrapper<ResidentCredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setErrors(List.of(new ServiceError(ResidentErrorCode.VID_REQUEST_CARD_FAILED.getErrorCode(),
				ResidentErrorCode.VID_REQUEST_CARD_FAILED.getErrorMessage())));
		Mockito.when(residentServiceRestClient.postApi(any(), any(), any(), any())).thenReturn(responseWrapper);
		downloadCardService.getVidCardEventId("123", 0, LOCALE_EN_US);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void testGetVidCardEventIdWithApisResourceAccessException() throws BaseCheckedException {
		Mockito.when(vidService.retrieveVids(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
				.thenThrow(new ApisResourceAccessException());
		downloadCardService.getVidCardEventId("123", 0, LOCALE_EN_US);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetVidCardEventIdWithIOException() throws BaseCheckedException, IOException {
		Mockito.when(identityService.getIdentityAttributes(Mockito.anyString(), isNull())).thenThrow(new IOException());
		downloadCardService.getVidCardEventId("123", 0, LOCALE_EN_US);
	}

	@Test
	public void testGetIndividualIdStatus()
			throws ApisResourceAccessException, IOException, ResidentServiceCheckedException {
		Map<String, String> packetStatusMap = new HashMap<>();
		packetStatusMap.put(ResidentConstants.AID_STATUS, PacketStatus.SUCCESS.name());
		packetStatusMap.put(ResidentConstants.TRANSACTION_TYPE_CODE, TransactionStage.CARD_READY_TO_DOWNLOAD.name());
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenReturn(packetStatusMap);
		ResponseWrapper<CheckStatusResponseDTO> individualIdStatus = downloadCardService
				.getIndividualIdStatus("3425636374");
		assertEquals(PacketStatus.SUCCESS.getName(), individualIdStatus.getResponse().getAidStatus());
	}

	@Test
	public void testGetIndividualIdStatusWithResidentCredentialServiceException()
			throws ApisResourceAccessException, IOException, ResidentServiceCheckedException {
		Map<String, String> packetStatusMap = new HashMap<>();
		packetStatusMap.put(ResidentConstants.AID_STATUS, PacketStatus.SUCCESS.name());
		packetStatusMap.put(ResidentConstants.TRANSACTION_TYPE_CODE, TransactionStage.CARD_READY_TO_DOWNLOAD.name());
		Mockito.when(utilities.getPacketStatus(Mockito.anyString())).thenReturn(packetStatusMap);
		Mockito.when(residentCredentialService.getDataShareUrl(Mockito.anyString()))
				.thenThrow(ResidentCredentialServiceException.class);
		ResponseWrapper<CheckStatusResponseDTO> individualIdStatus = downloadCardService
				.getIndividualIdStatus("3425636374");
		assertEquals(PacketStatus.IN_PROGRESS.getName(), individualIdStatus.getResponse().getAidStatus());
	}
}