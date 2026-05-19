package com.example.daugia.common.location;

import com.example.daugia.common.dto.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private static final String BASE_URL = "https://provinces.open-api.vn/api/v2";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceDto>>> getProvinces() {
        List<ProvinceDto> provinces = fetchList(
                BASE_URL + "/p/",
                new ParameterizedTypeReference<List<ProvinceDto>>() {}
        );
        return ResponseEntity.ok(ApiResponse.success("Provinces fetched", provinces));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardDto>>> getWards(@RequestParam("provinceCode") int provinceCode) {
        String url = BASE_URL + "/w/?province=" + provinceCode;
        List<WardDto> wards = fetchList(url, new ParameterizedTypeReference<List<WardDto>>() {});
        return ResponseEntity.ok(ApiResponse.success("Wards fetched", wards));
    }

    private <T> List<T> fetchList(String url, ParameterizedTypeReference<List<T>> typeRef) {
        ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, null, typeRef);
        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    public static class ProvinceDto {
        private Integer code;
        private String name;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class WardDto {
        private Integer code;
        private String name;
        private Integer province_code;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getProvince_code() {
            return province_code;
        }

        public void setProvince_code(Integer province_code) {
            this.province_code = province_code;
        }
    }
}
