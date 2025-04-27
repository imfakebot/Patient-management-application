package com.pma.service; // Package chứa các lớp Service

import com.pma.model.entity.Department; // Import Entity Department
import com.pma.repository.DepartmentRepository; // Import Repository Department
import com.pma.repository.DoctorRepository; // Import Repository Doctor để kiểm tra ràng buộc
import jakarta.persistence.EntityNotFoundException; // Exception chuẩn khi không tìm thấy Entity
import org.slf4j.Logger; // Import Logger để ghi log (thực hành tốt)
import org.slf4j.LoggerFactory; // Import Logger Factory
import org.springframework.beans.factory.annotation.Autowired; // Annotation để tiêm dependency
import org.springframework.dao.DataIntegrityViolationException; // Exception khi vi phạm ràng buộc DB
import org.springframework.stereotype.Service; // Đánh dấu là Spring Service Bean
import org.springframework.transaction.annotation.Isolation; // Tùy chọn: Chỉ định mức cô lập Transaction
import org.springframework.transaction.annotation.Propagation; // Tùy chọn: Chỉ định cách lan truyền Transaction
import org.springframework.transaction.annotation.Transactional; // Annotation cốt lõi để quản lý Transaction

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp Service cung cấp các logic nghiệp vụ và quản lý giao dịch
 * cho các hoạt động liên quan đến thực thể Department.
 * Tương tác với tầng Repository để truy cập dữ liệu.
 */
@Service // Đánh dấu lớp này là một Service Component, được Spring quản lý
public class DepartmentService {

    // Khởi tạo Logger cho lớp này
    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    // Khai báo các Repository cần thiết dưới dạng final để đảm bảo chúng được khởi
    // tạo
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Sử dụng Constructor Injection để tiêm (inject) các dependency Repository.
     * Đây là cách được khuyến nghị trong Spring vì nó đảm bảo các dependency bắt
     * buộc
     * được cung cấp khi Service được tạo ra và giúp việc viết unit test dễ dàng
     * hơn.
     * Spring sẽ tự động tìm các bean phù hợp (DepartmentRepository,
     * DoctorRepository)
     * và truyền chúng vào constructor này.
     *
     * @param departmentRepository Bean Repository cho Department.
     * @param doctorRepository     Bean Repository cho Doctor (cần để kiểm tra khi
     *                             xóa Department).
     */
    @Autowired // Đánh dấu constructor này để Spring tự động tiêm dependency
    public DepartmentService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Lấy danh sách tất cả các Department có trong cơ sở dữ liệu.
     * Phương thức này chỉ đọc dữ liệu, nên sử dụng @Transactional(readOnly = true)
     * để tối ưu hóa hiệu năng và báo cho JPA/Hibernate biết không cần thực hiện
     * dirty checking hoặc chuẩn bị cho việc ghi dữ liệu.
     *
     * @return Một List chứa tất cả các đối tượng Department. Trả về danh sách rỗng
     *         nếu không có Department nào.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS) // SUPPORTS: Tham gia transaction nếu có, không
                                                                        // thì chạy không cần transaction
    public List<Department> getAllDepartments() {
        log.info("Fetching all departments");
        List<Department> departments = departmentRepository.findAll();
        log.info("Found {} departments", departments.size());
        return departments;
    }

    /**
     * Tìm kiếm một Department cụ thể dựa vào ID (UUID) của nó.
     *
     * @param id UUID của Department cần tìm.
     * @return Đối tượng Department nếu tìm thấy.
     * @throws EntityNotFoundException nếu không có Department nào khớp với ID được
     *                                 cung cấp.
     *                                 Việc ném exception giúp tầng trên
     *                                 (Controller) xử lý lỗi rõ ràng (ví dụ: trả về
     *                                 HTTP 404).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Department getDepartmentById(UUID id) {
        log.info("Fetching department with id: {}", id);
        // findById trả về Optional, sử dụng orElseThrow để ném exception nếu Optional
        // rỗng.
        return departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Department not found with id: {}", id);
                    return new EntityNotFoundException("Department not found with id: " + id);
                });
    }

    /**
     * Tìm kiếm một Department cụ thể dựa vào tên của nó (không phân biệt chữ hoa
     * chữ thường).
     *
     * @param name Tên của Department cần tìm.
     * @return Optional chứa đối tượng Department nếu tìm thấy, Optional rỗng nếu
     *         không.
     *         Trả về Optional cho phép tầng gọi quyết định cách xử lý khi không tìm
     *         thấy.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Department> findDepartmentByNameIgnoreCase(String name) {
        log.info("Searching for department with name (case-insensitive): {}", name);
        return departmentRepository.findByDepartmentNameIgnoreCase(name);
    }

    /**
     * Tạo một Department mới trong cơ sở dữ liệu.
     * Thực hiện kiểm tra logic nghiệp vụ cơ bản (tên không được trùng).
     * 
     * @Transactional đảm bảo thao tác được thực hiện trong một giao dịch. Nếu có
     *                lỗi,
     *                mọi thay đổi trong transaction sẽ được rollback.
     *
     * @param department Đối tượng Department chứa thông tin cần tạo. ID nên là
     *                   null.
     * @return Department đã được lưu vào cơ sở dữ liệu (với ID và timestamp đã được
     *         gán).
     * @throws IllegalArgumentException nếu tên Department đã tồn tại (không phân
     *                                  biệt hoa thường).
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED) // REQUIRED: Luôn chạy
                                                                                             // trong transaction mới
                                                                                             // hoặc tham gia
                                                                                             // transaction hiện có.
                                                                                             // READ_COMMITTED là mức cô
                                                                                             // lập phổ biến.
    public Department createDepartment(Department department) {
        log.info("Attempting to create department with name: {}", department.getDepartmentName());

        // --- Business Logic Validation ---
        // Kiểm tra xem tên đã tồn tại chưa (bỏ qua hoa thường)
        findDepartmentByNameIgnoreCase(department.getDepartmentName()).ifPresent(_ -> {
            log.warn("Department creation failed. Name already exists: {}", department.getDepartmentName());
            throw new IllegalArgumentException(
                    "Department name '" + department.getDepartmentName() + "' already exists.");
        });

        // Đảm bảo ID là null để Hibernate biết đây là entity mới cần tạo ID
        if (department.getDepartmentId() != null) {
            log.warn("Attempted to create a department with a pre-set ID: {}. Setting ID to null.",
                    department.getDepartmentId());
            department.setDepartmentId(null); // An toàn hơn là để null
        }

        // --- Data Access ---
        // Lưu entity mới vào DB
        Department savedDepartment = departmentRepository.save(department);
        log.info("Successfully created department with id: {} and name: {}", savedDepartment.getDepartmentId(),
                savedDepartment.getDepartmentName());
        return savedDepartment;
    }

    /**
     * Cập nhật thông tin của một Department đã tồn tại (ví dụ: chỉ cập nhật tên).
     *
     * @param id             UUID của Department cần cập nhật.
     * @param updatedDetails Đối tượng Department chứa thông tin mới (ví dụ: tên
     *                       mới).
     *                       Các trường khác không được cập nhật ở đây (như ID,
     *                       timestamps).
     * @return Department đã được cập nhật.
     * @throws EntityNotFoundException  nếu không tìm thấy Department với ID cung
     *                                  cấp.
     * @throws IllegalArgumentException nếu tên mới xung đột với một Department khác
     *                                  đã tồn tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Department updateDepartment(UUID id, Department updatedDetails) {
        log.info("Attempting to update department with id: {}", id);

        // 1. Lấy Entity hiện có từ DB. Nó sẽ ở trạng thái 'managed' bởi EntityManager.
        Department existingDepartment = getDepartmentById(id); // Ném EntityNotFoundException nếu không tồn tại

        // --- Business Logic Validation ---
        String newName = updatedDetails.getDepartmentName();
        // Chỉ kiểm tra trùng tên nếu tên thực sự thay đổi và khác tên hiện tại (bỏ qua
        // hoa thường)
        if (newName != null && !newName.trim().isEmpty()
                && !existingDepartment.getDepartmentName().equalsIgnoreCase(newName.trim())) {
            log.info("Department name change detected. Old: '{}', New: '{}'. Checking for conflicts.",
                    existingDepartment.getDepartmentName(), newName.trim());
            Optional<Department> conflictingDept = departmentRepository.findByDepartmentNameIgnoreCase(newName.trim());
            // Nếu tìm thấy department khác có tên mới này -> Lỗi
            if (conflictingDept.isPresent()) {
                log.warn("Department update failed. New name '{}' conflicts with existing department id: {}",
                        newName.trim(), conflictingDept.get().getDepartmentId());
                throw new IllegalArgumentException(
                        "Department name '" + newName.trim() + "' is already in use by another department.");
            }
            // --- Cập nhật Entity ---
            // Cập nhật tên trên đối tượng 'managed'
            existingDepartment.setDepartmentName(newName.trim());
            log.info("Department name updated for id: {}", id);
        } else {
            log.info("No name change detected or new name is invalid/same for department id: {}", id);
        }

        // --- Lưu thay đổi ---
        // Do 'existingDepartment' đang ở trạng thái 'managed', khi transaction kết
        // thúc,
        // Hibernate sẽ tự động phát hiện thay đổi (dirty checking) và tạo câu lệnh
        // UPDATE.
        // Việc gọi save() rõ ràng thường không cần thiết cho update, nhưng cũng không
        // sai.
        // Department savedDepartment = departmentRepository.save(existingDepartment);
        log.info("Department update process completed for id: {}", id);
        return existingDepartment; // Trả về entity đã được cập nhật (trong trạng thái managed)
    }

    /**
     * Xóa một Department khỏi cơ sở dữ liệu dựa vào ID.
     * Thực hiện kiểm tra ràng buộc: không cho phép xóa nếu vẫn còn Doctor thuộc về
     * Department đó.
     *
     * @param id UUID của Department cần xóa.
     * @throws EntityNotFoundException         nếu không tìm thấy Department với ID
     *                                         cung cấp.
     * @throws IllegalStateException           nếu Department không thể xóa do còn
     *                                         Doctor liên kết.
     * @throws DataIntegrityViolationException nếu có lỗi ràng buộc khóa ngoại khác
     *                                         (ít khả năng xảy ra nếu đã kiểm tra
     *                                         Doctor).
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteDepartment(UUID id) {
        log.info("Attempting to delete department with id: {}", id);

        // 1. Lấy thông tin department (hoặc kiểm tra tồn tại) để chắc chắn nó có tồn
        // tại
        Department departmentToDelete = getDepartmentById(id); // Ném EntityNotFoundException nếu không tồn tại

        // --- Business Logic Validation (Kiểm tra ràng buộc khóa ngoại) ---
        // Sử dụng phương thức count trong DoctorRepository để kiểm tra hiệu quả
        // **Yêu cầu: Phải thêm `long countByDepartment_DepartmentId(UUID
        // departmentId);` vào DoctorRepository**
        long associatedDoctorsCount = doctorRepository.countByDepartment_DepartmentId(id);
        if (associatedDoctorsCount > 0) {
            log.warn("Deletion failed for department id: {}. Found {} associated doctors.", id, associatedDoctorsCount);
            throw new IllegalStateException("Cannot delete department '" + departmentToDelete.getDepartmentName() +
                    "' (ID: " + id + ") because " + associatedDoctorsCount +
                    " doctor(s) are still associated with it.");
        }

        // --- Data Access ---
        // Nếu không có ràng buộc, tiến hành xóa
        departmentRepository.delete(departmentToDelete); // Hoặc deleteById(id)
        log.info("Successfully deleted department with id: {}", id);
    }
}