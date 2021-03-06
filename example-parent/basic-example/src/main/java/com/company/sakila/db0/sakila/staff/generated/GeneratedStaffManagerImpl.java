package com.company.sakila.db0.sakila.staff.generated;

import com.company.sakila.db0.sakila.staff.Staff;
import com.company.sakila.db0.sakila.staff.StaffManager;
import com.speedment.common.annotation.GeneratedCode;
import com.speedment.runtime.config.identifier.TableIdentifier;
import com.speedment.runtime.core.manager.AbstractManager;
import com.speedment.runtime.field.Field;
import java.util.stream.Stream;

/**
 * The generated base implementation for the manager of every {@link
 * com.company.sakila.db0.sakila.staff.Staff} entity.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@GeneratedCode("Speedment")
public abstract class GeneratedStaffManagerImpl 
extends AbstractManager<Staff> 
implements GeneratedStaffManager {
    
    private final TableIdentifier<Staff> tableIdentifier;
    
    protected GeneratedStaffManagerImpl() {
        this.tableIdentifier = TableIdentifier.of("db0", "sakila", "staff");
    }
    
    @Override
    public TableIdentifier<Staff> getTableIdentifier() {
        return tableIdentifier;
    }
    
    @Override
    public Stream<Field<Staff>> fields() {
        return StaffManager.FIELDS.stream();
    }
    
    @Override
    public Stream<Field<Staff>> primaryKeyFields() {
        return Stream.of(
            Staff.STAFF_ID
        );
    }
}