package com.pma.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.mapping.Set;

/**
 * Represents a patient entity in the system.
 * 
 * This class contains personal, medical, and contact information about a
 * patient.
 * It also maintains relationships with other entities such as appointments,
 * prescriptions,
 * medical records, bills, and user accounts.
 */
@Entity
@Table(name = "patient")
public class Patient {
    /**
     * The unique identifier for the patient.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "patient_id", updatable = false, nullable = false)
    private UUID patientID;

    /**
     * The full name of the patient.
     */
    @Column(name = "full_name", length = 255, nullable = false)
    private String patientName;

    /**
     * The date of birth of the patient.
     */
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /**
     * The gender of the patient.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    /**
     * The phone number of the patient.
     */
    @Column(name = "phone", unique = true, length = 15, nullable = false)
    private String phone;

    /**
     * The email address of the patient.
     */
    @Column(name = "email", unique = true, length = 255, nullable = true)
    private String email;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100, nullable = true)
    private String state;

    @Column(name = "postal_code", length = 20, nullable = true)
    private String postalCode;

    @Column(name = "country", length = 50, nullable = true)
    private String country;

    @Column(name = "blood_type", length = 3)
    private String bloodType;

    @Lob
    @Column(name = "allergies", columnDefinition = "NVARCHAR(MAX)")
    private String allergies;

    @Lob
    @Column(name = "medical_history", columnDefinition = "NVARCHAR(MAX)")
    private String medicalHistory;

    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    private String emergencyContactPhone;

    /**
     * The timestamp when the patient record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * The timestamp when the patient record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * The collection of appointments associated with this patient.
     * This is a bidirectional OneToMany relationship where this patient is the
     * owner.
     * The collection is fetched lazily and configured with cascade ALL operations.
     * Orphan removal is enabled to automatically delete appointments when they're
     * removed from the collection.
     */
    @OneToMany(mappedBy = "patient", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Appointment> appointments = new HashSet<>();

    /**
     * The collection of prescriptions issued to this patient.
     * This is a bidirectional OneToMany relationship where this patient is the
     * owner.
     * The collection is fetched lazily and configured with cascade ALL operations.
     * Orphan removal is enabled to automatically delete prescriptions when they're
     * removed from the collection.
     */
    @OneToMany(mappedBy = "patient", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Prescription> prescriptions = new HashSet<>();

    /**
     * The collection of medical records belonging to this patient.
     * This is a bidirectional OneToMany relationship where this patient is the
     * owner.
     * The collection is fetched lazily and configured with cascade ALL operations.
     * Orphan removal is enabled to automatically delete medical records when
     * they're removed from the collection.
     */
    @OneToMany(mappedBy = "patient", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<MedicalRecord> medicalRecords = new HashSet<>();

    /**
     * The collection of bills associated with this patient.
     * This is a bidirectional OneToMany relationship where this patient is the
     * owner.
     * The collection is fetched lazily and configured with cascade ALL operations.
     * Orphan removal is enabled to automatically delete bills when they're removed
     * from the collection.
     */
    @OneToMany(mappedBy = "patient", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Bill> bills = new HashSet<>();

    /**
     * The user account associated with this patient.
     * This is a bidirectional OneToOne relationship where this patient is the
     * owner.
     * The relationship is fetched lazily and configured with cascade ALL
     * operations.
     * Orphan removal is enabled to automatically delete the user account when it's
     * unset.
     */
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserAccount userAccount;

    /**
     * Default constructor for the Patient class.
     * Required by JPA specification and used by the JPA provider.
     * Initializes collection fields to empty HashSets.
     */
    public Patient() {
    }

    public UUID getPatientID() {
        return patientID;
    }

    public void setPatientID(UUID patientID) {
        this.patientID = patientID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp of the last update to the patient record.
     * 
     * @return the timestamp of the last update.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp of the last update to the patient record.
     * 
     * @param updatedAt the timestamp to set.
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the set of appointments associated with the patient.
     * 
     * @return a set of appointments.
     */
    public Set<Appointment> getAppointments() {
        return appointments;
    }

    /**
     * Sets the set of appointments associated with the patient.
     * 
     * @param appointments the set of appointments to associate.
     */
    public void setAppointments(Set<Appointment> appointments) {
        this.appointments = appointments;
    }

    /**
     * Gets the set of prescriptions associated with the patient.
     * 
     * @return a set of prescriptions.
     */
    public Set<Prescription> getPrescriptions() {
        return prescriptions;
    }

    /**
     * Sets the set of prescriptions associated with the patient.
     * 
     * @param prescriptions the set of prescriptions to associate.
     */
    public void setPrescriptions(Set<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    /**
     * Gets the set of medical records associated with the patient.
     * 
     * @return a set of medical records.
     */
    public Set<MedicalRecord> getMedicalRecords() {
        return medicalRecords;
    }

    /**
     * Sets the set of medical records associated with the patient.
     * 
     * @param medicalRecords the set of medical records to associate.
     */
    public void setMedicalRecords(Set<MedicalRecord> medicalRecords) {
        this.medicalRecords = medicalRecords;
    }

    public Set<Bill> getBills() {
        return bills;
    }

    public void setBills(Set<Bill> bills) {
        this.bills = bills;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    /**
     * Compares this patient with another object for equality based on the patient
     * ID.
     * 
     * @param o the object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Patient patient = (Patient) o;
        return patientID != null && patientID.equals(patient.patientID);
    }

    /**
     * Computes the hash code for the patient based on the patient ID.
     * 
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return patientID != null ? Objects.hash(patientID) : getClass().hashCode();
    }
}
