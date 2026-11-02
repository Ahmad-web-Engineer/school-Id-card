package com.platform.idcard.settings.repository;

import com.platform.idcard.settings.entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {

    List<PlatformSetting> findByCategory(String category);
}
