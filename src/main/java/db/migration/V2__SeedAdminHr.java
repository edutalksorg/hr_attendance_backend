package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;

public class V2__SeedAdminHr extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {

        String sql = "INSERT INTO users (id, full_name, email, password_hash, role, status)\n" +
            "VALUES\n" +
            "(gen_random_uuid(), 'Admin', 'megamart.dvst@gmail.com', crypt('edutalks@321', gen_salt('bf')), 'ADMIN', 'ACTIVE'),\n" +
            "(gen_random_uuid(), 'HR Manager', 'hr@megamart.com', crypt('Hr123@', gen_salt('bf')), 'HR', 'ACTIVE');";

        try (PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
            ps.execute();
        }
    }
}
