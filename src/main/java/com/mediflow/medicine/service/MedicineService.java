package com.mediflow.medicine.service;

import com.mediflow.medicine.dto.MedicineRequestDto;
import com.mediflow.medicine.dto.MedicineResponseDto;
import com.mediflow.medicine.entity.Medicine;
import com.mediflow.medicine.exception.MedicineNotFoundException;
import com.mediflow.medicine.repository.MedicineRepository;
import com.mediflow.medicine.specification.MedicineSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MedicineService {

    //Creating Reference Variable
    private final MedicineRepository medicineRepository;

    //Injectig Medicine Repo by Constructor
    public MedicineService(MedicineRepository medicineRepository) {

        // what Repo we get in contructor Parameter , now we store into variable below line
        this.medicineRepository = medicineRepository;

        //left one Service Variable = right one is contructor parameter
/*

       [ public class Student {
            private String name;

            public Student(String name) {
                this.name = name;
            }
        }

        Student student = new Student("Pulak");  ]
*/
    }

    // Medicine -> Return Type ,Medicine  → Parameter Type ,medicine  → Parameter Name / Reference Variable
  /*  public Medicine createMedicine(Medicine medicine) {

        if (medicine.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        if (medicine.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

    *//*     public int add(int a, int b){
             return a + b;
         }
         int → Return Type;
         return a + b;
         save(medicine) → Medicine
         *//*

        return medicineRepository.save(medicine);
    }*/

    //Get all Medicine
   /* public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll(); //here findAll()- build method->Data JPA, which is called from Repo
    }*/

    //get all medicine
    /*public List<MedicineResponseDto> getAllMedicines() {

        List<Medicine> medicines = medicineRepository.findAll();

        return medicines.stream()
                .map(this::mapToResponseDto)
                .toList();
    }*/

    public Page<MedicineResponseDto> getAllMedicines(Pageable pageable) {

        Page<Medicine> medicines =
                medicineRepository.findAll(pageable);

        return medicines.map(this::mapToResponseDto);
    }

    //get single medicine
    /*public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() ->
                        new MedicineNotFoundException("Medicine not found with id " + id));
    }*/
    //GET BY ID
    public MedicineResponseDto getMedicineById(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new MedicineNotFoundException(
                                "Medicine not found with id: " + id));

        return mapToResponseDto(medicine);
    }

    // update particular medicine
    public MedicineResponseDto updateMedicine(Long id, MedicineRequestDto request) {
        Medicine existingMedicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new MedicineNotFoundException("Medicine not found with this id:" + id));
        // Update existing medicine with new values
        existingMedicine.setName(request.getName());
        existingMedicine.setDescription(request.getDescription());
        existingMedicine.setCategory(request.getCategory());
        existingMedicine.setManufacturer(request.getManufacturer());
        existingMedicine.setPrice(request.getPrice());
        existingMedicine.setStockQuantity(request.getStockQuantity());
        existingMedicine.setExpiryDate(request.getExpiryDate());
        existingMedicine.setPrescriptionRequired(
                request.getPrescriptionRequired()
        );
        existingMedicine.setStatus(request.getStatus());

        Medicine updatedMedicine = medicineRepository.save(existingMedicine);// এখানে আমরা updated object save করছি।
        return mapToResponseDto(updatedMedicine);
    }

    // DELETE
    public void deleteMedicine(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new MedicineNotFoundException(
                                "medicine not found with this is" + id
                        ));
        medicineRepository.delete(medicine);
    }

    // api search medicine
   /* public List<Medicine> searchMedicines(String name){
        return medicineRepository.findByNameContainingIgnoreCase(name);
    }*/

    public Page<MedicineResponseDto> searchMedicines(
            String name,
            Pageable pageable) {

        Page<Medicine> medicines =
                medicineRepository
                        .findByNameContainingIgnoreCase(
                                name, pageable);

        return medicines.map(this::mapToResponseDto);
    }

   /* public List<MedicineResponseDto> searchMedicines(String name) {

        List<Medicine> medicines =
                medicineRepository.findByNameContainingIgnoreCase(name);

        return medicines.stream()
                .map(this::mapToResponseDto)
                .toList();
    }*/


    //CREATE
    public MedicineResponseDto createMedicine(
            MedicineRequestDto request) {

        Medicine medicine = new Medicine();

        medicine.setName(request.getName());
        medicine.setDescription(request.getDescription());
        medicine.setCategory(request.getCategory());
        medicine.setManufacturer(request.getManufacturer());
        medicine.setPrice(request.getPrice());
        medicine.setStockQuantity(request.getStockQuantity());
        medicine.setExpiryDate(request.getExpiryDate());
        medicine.setPrescriptionRequired(
                request.getPrescriptionRequired()
        );
        medicine.setStatus(request.getStatus());

        Medicine savedMedicine =
                medicineRepository.save(medicine);

        return mapToResponseDto(savedMedicine);
    }

    private MedicineResponseDto mapToResponseDto(Medicine medicine) {

        return new MedicineResponseDto(
                medicine.getId(),
                medicine.getName(),
                medicine.getDescription(),
                medicine.getCategory(),
                medicine.getManufacturer(),
                medicine.getPrice(),
                medicine.getStockQuantity(),
                medicine.getExpiryDate(),
                medicine.getPrescriptionRequired(),
                medicine.getStatus()
        );
    }

    public Page<MedicineResponseDto> getMedicinesByCategory(
            String category,
            Pageable pageable) {

        Page<Medicine> medicines =
                medicineRepository.findByCategoryIgnoreCase(
                        category, pageable);

        return medicines.map(this::mapToResponseDto);
    }

    // make filter dynamic or we can put multiple filter at a time
    public Page<MedicineResponseDto> filterMedicines(
            String category,
            String manufacturer,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        Specification<Medicine> specification =
                MedicineSpecification.filter(
                        category,
                        manufacturer,
                        minPrice,
                        maxPrice
                );

        Page<Medicine> medicines =
                medicineRepository.findAll(
                        specification,
                        pageable
                );

        return medicines.map(this::mapToResponseDto);
    }
}