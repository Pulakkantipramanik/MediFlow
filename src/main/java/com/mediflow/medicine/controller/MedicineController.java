package com.mediflow.medicine.controller;

import com.mediflow.medicine.dto.MedicineRequestDto;
import com.mediflow.medicine.dto.MedicineResponseDto;
import com.mediflow.medicine.entity.Medicine;
import com.mediflow.medicine.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;


    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

   /* @PostMapping
    public Medicine createMedicine(
            @Valid @RequestBody Medicine medicine) {

        return medicineService.createMedicine(medicine);
    }*/


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MedicineResponseDto> createMedicine(
            @Valid @RequestBody MedicineRequestDto request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicineService.createMedicine(request));
    }

   /* @PostMapping
    public MedicineResponseDto  createMedicine(@Valid @RequestBody MedicineRequestDto request) //Medicine is the input type that we pass from Postman as Json Body
    {*/
        //@Valid annotaion -> Validation rules check (we use in entity class like @notNul, @NotBlank
  /*      Postman JSON
     ↓
        @RequestBody
     ↓
        Medicine object তৈরি
     ↓
        @Valid
     ↓
        Validation rules check
     ↓
 ┌───────────────┐
 │ price = -10   │ → @Positive ❌
 │ stock = -5    │ → @Min(0) ❌
 └───────────────┘
     ↓
        Validation Exception
     ↓
        MethodArgumentNotValidException*/
    /*    return medicineService.createMedicine(request);
    }*/
  /*  Controller → getAllMedicines()
                  ↓
    Service    → getAllMedicines()
                  ↓
    Repository → findAll()

    Call chain-টাই আসল, নাম same হওয়া নয়।*/

  /*  @GetMapping
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();//here always match method name with service class
    }
*/
  /* @GetMapping
    public List<MedicineResponseDto> getAllMedicines() {
        return medicineService.getAllMedicines();
    }*/
/*  @GetMapping
  public Page<MedicineResponseDto> getAllMedicines(
          Pageable pageable) {

      return medicineService.getAllMedicines(pageable);
  }*/
  @GetMapping
  public Page<MedicineResponseDto> getAllMedicines(

          @RequestParam(defaultValue = "0") int page,

          @RequestParam(defaultValue = "10") int size,

          @RequestParam(defaultValue = "name") String sortBy,

          @RequestParam(defaultValue = "asc") String direction) {

      Pageable pageable =
              createPageable(page, size, sortBy, direction);

      return medicineService.getAllMedicines(pageable);
  }


    @GetMapping("/{id}")
    public MedicineResponseDto getMedicineById(@PathVariable Long id) {
        return medicineService.getMedicineById(id);
    }
    /*      | Operation    | HTTP Method    | What do we pass?     | Where does it come from?         |
            | ------------ | -------------- | -------------------- | -------------------------------- |
            | **Create**   | `POST`         | Object / JSON        | `@RequestBody`                   |
            | **Read All** | `GET`          | Nothing              | —                                |
            | **Read One** | `GET /{id}`    | `id`                 | `@PathVariable`                  |
            | **Update**   | `PUT /{id}`    | `id` + Object / JSON | `@PathVariable` + `@RequestBody` |
            | **Delete**   | `DELETE /{id}` | `id`                 | `@PathVariable`                  |

*/
   /* @PutMapping("/{id}")
    public Medicine updateMedicine
    (@PathVariable Long id , @Valid @RequestBody Medicine medicine){
      return medicineService.updateMedicine(id,medicine);
    }*/
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public MedicineResponseDto updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineRequestDto request) {

        return medicineService.updateMedicine(id, request);
    }

   /* @DeleteMapping("/{id}")
    public void deleteMedicine(@PathVariable Long id){
         medicineService.deleteMedicine(id);
    }*/

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(
            @PathVariable Long id) {

        medicineService.deleteMedicine(id);

        return ResponseEntity.noContent().build();
    }

   /* /api/medicines/search ? name = PAra
        ↓              ↓    ↓    ↓
    path           query  |  value
            parameter*/

   /* @GetMapping("/search")
    public List<Medicine> searchMedicines(
            @RequestParam String name){
        return medicineService.searchMedicines(name);
    }*/

    //searching
 /*  @GetMapping("/search")
   public List<MedicineResponseDto> searchMedicines(
           @RequestParam String name) {

       return medicineService.searchMedicines(name);
   }*/
/*   @GetMapping("/search")
   public Page<MedicineResponseDto> searchMedicines(
           @RequestParam String name,
           Pageable pageable) {

       return medicineService.searchMedicines(
               name, pageable);
   }*/
    @GetMapping("/search")
    public Page<MedicineResponseDto> searchMedicines(

            @RequestParam String name,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "name") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        return medicineService.searchMedicines(
                name, pageable);
    }
    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 50) {
            size = 50;
        }

        List<String> allowedFields = List.of(
                "name",
                "price",
                "stockQuantity",
                "expiryDate"
        );

        if (!allowedFields.contains(sortBy)) {
            sortBy = "name";
        }

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        return PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );
    }
    @GetMapping("/category")
    public Page<MedicineResponseDto> getMedicinesByCategory(
            @RequestParam String category,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "name") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        return medicineService.getMedicinesByCategory(
                category, pageable);
    }
    @GetMapping("/filter")
    public Page<MedicineResponseDto> filterMedicines(

            @RequestParam(required = false) String category,

            @RequestParam(required = false) String manufacturer,

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "name") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        Pageable pageable =
                createPageable(page, size, sortBy, direction);

        return medicineService.filterMedicines(
                category,
                manufacturer,
                minPrice,
                maxPrice,
                pageable);
    }

}
