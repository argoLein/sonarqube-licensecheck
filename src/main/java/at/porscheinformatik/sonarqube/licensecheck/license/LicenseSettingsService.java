package at.porscheinformatik.sonarqube.licensecheck.license;

import static at.porscheinformatik.sonarqube.licensecheck.LicenseCheckPropertyKeys.LICENSE_KEY;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.server.platform.PersistentSettings;

@ServerSide
public class LicenseSettingsService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseSettingsService.class);

    /**
     * This is not official API
     */
    private final PersistentSettings persistentSettings;

    private final Settings settings;
    private final LicenseService licenseService;

    public LicenseSettingsService(PersistentSettings persistentSettings, LicenseService licenseService)
    {
        super();
        this.persistentSettings = persistentSettings;
        this.settings = persistentSettings.getSettings();
        this.licenseService = licenseService;

        initSpdxLicences();
    }

    public String getLicensesID()
    {
        List<License> licenses = licenseService.getLicenses();

        StringBuilder licenseString = new StringBuilder();
        for (License license : licenses)
        {
            licenseString.append(license.getIdentifier()).append(";");
        }
        return licenseString.toString();
    }

    public boolean addLicense(String name, String identifier, String status)
    {
        License newLicense = new License(name, identifier, status);
        return addLicense(newLicense);
    }

    public boolean addLicense(License newLicense)
    {
        List<License> licenses = licenseService.getLicenses();

        if (listContains(newLicense, licenses))
        {
            return false;
        }

        licenses.add(newLicense);
        saveSettings(licenses);

        return true;
    }

    private boolean listContains(License newLicense, List<License> licenses)
    {
        for (License license : licenses)
        {
            if (newLicense.getIdentifier().equals(license.getIdentifier()))
            {
                return true;
            }
        }
        return false;
    }

    public boolean deleteLicense(String id)
    {
        List<License> licenses = License.fromString(settings.getString(LICENSE_KEY));
        List<License> newLicenseList = new ArrayList<>();
        boolean found = false;
        for (License license : licenses)
        {
            if (id.equals(license.getIdentifier()))
            {
                found = true;
            }
            else
            {
                newLicenseList.add(license);
            }
        }

        if (found)
        {
            saveSettings(newLicenseList);
        }

        return found;
    }

    public boolean updateLicense(final String id, final String newName, final String newStatus)
    {
        List<License> licenses = licenseService.getLicenses();

        for (License license : licenses)
        {
            if (id.equals(license.getIdentifier()))
            {
                license.setName(newName);
                license.setStatus(newStatus);
                saveSettings(licenses);
                return true;
            }
        }
        return false;
    }

    private void saveSettings(List<License> licenseList)
    {
        Collections.sort(licenseList);
        String licenseJson = License.createString(licenseList);
        settings.setProperty(LICENSE_KEY, licenseJson);
        persistentSettings.saveProperty(LICENSE_KEY, licenseJson);
    }

    private void initSpdxLicences()
    {
        String licenseJson = settings.getString(LICENSE_KEY);

        if ((licenseJson != null) && !licenseJson.isEmpty())
        {
            return;
        }

        try
        {
            InputStream inputStream = LicenseService.class.getResourceAsStream("spdx_license_list.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder out = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
            {
                out.append(line);
            }

            String newJsonLicense = out.toString();
            settings.setProperty(LICENSE_KEY, newJsonLicense);
            persistentSettings.saveProperty(LICENSE_KEY, newJsonLicense);

            reader.close();
            inputStream.close();
        }
        catch (Exception e)
        {
            LOGGER.error("Could not load spdx_license_list.json", e);
        }
    }
}
