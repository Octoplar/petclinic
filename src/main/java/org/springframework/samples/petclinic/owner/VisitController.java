/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.lang.Nullable;
import org.springframework.samples.petclinic.model.Person;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.Vets;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

    private final VisitRepository visits;
    private final PetRepository pets;
    private final VetRepository vets;
    private final OwnerRepository owners;


    public VisitController(VisitRepository visits, PetRepository pets, VetRepository vets, OwnerRepository owners) {
        this.visits = visits;
        this.pets = pets;
        this.vets = vets;
        this.owners=owners;
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
        dataBinder.setDisallowedFields("aborted");
    }

    /**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @return Pet
     */
    @ModelAttribute("visit")
    public Visit loadPetWithVisit() {
        return new Visit();
    }

    @ModelAttribute("vets")
    public Vets loadVets() {
        Vets vets = new Vets();
        vets.getVetList().addAll(this.vets.findAll());
        return vets;
    }

    // Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
    @GetMapping("/owners/*/pets/{petId}/visits/new")
    public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
        Pet pet = this.pets.findById(petId);
        model.put("pet", pet);
        return "pets/createOrUpdateVisitForm";
    }

    // Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String processNewVisitForm(@Valid Visit visit, @RequestParam(name = "vetString") String vetString, BindingResult result, @PathVariable("petId") int petId, Map<String, Object> model) {
        Pet pet = this.pets.findById(petId);
        model.put("pet", pet);
        if (result.hasErrors()) {
            return "pets/createOrUpdateVisitForm";
        } else {
            visit.setVet(strToVet(vetString));
            this.visits.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }

    // Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
    @GetMapping("/owners/{ownerId}/visits/{visitId}")
    public String showUpdateVisitForm(@PathVariable("ownerId") int ownerId, @PathVariable("visitId") int visitId, Map<String, Object> model) {
        Owner owner=owners.findById(ownerId);
        Visit visit=visits.findById(visitId);
        if (owner!=null && visit!=null && checkAccess(owner, visit)){
            model.put("owner", owner);
            model.put("visit", visit);
            return "pets/modifyVisitForm.html";
        }
        return "redirect:/owners/" + ownerId;
    }

    @PutMapping("/owners/{ownerId}/visits/{visitId}")
    public String processUpdateVisitForm(@Valid Visit newVisit,
                                         BindingResult result,
                                         @PathVariable("ownerId") int ownerId,
                                         @PathVariable("visitId") int visitId,
                                         @RequestParam(name = "vetString") String vetString) {
        newVisit.setVet(strToVet(vetString));
        Owner owner=owners.findById(ownerId);
        Visit visit=visits.findById(visitId);
        if (!result.hasErrors() //owner and visits exists and valid, owner has access to visit
            && owner!=null
            && visit!=null
            && checkAccess(owner, visit)
            && checkAccess(owner, newVisit)){

            //3 fields according task only
            visit.setVet(newVisit.getVet());
            visit.setPetId(newVisit.getPetId());
            visit.setDate(newVisit.getDate());

            this.visits.save(visit);
            return "redirect:/owners/" + ownerId;
        }
        return "redirect:/owners/" + ownerId;
    }

    @PutMapping("/owners/{ownerId}/visits/{visitId}/cancel")
    @ResponseBody
    public String processCancelVisitForm(@PathVariable("ownerId") int ownerId, @PathVariable("visitId") int visitId) {
        Owner owner=owners.findById(ownerId);
        Visit visit=visits.findById(visitId);
        if (owner!=null          //owner and visit exists, owner has access to visit
            && visit!=null
            && checkAccess(owner, visit)){
            visit.setAborted(true);
            this.visits.save(visit);
            return "successfully aborted";
        }
        return "fail";
    }

    /**
     * Private converter with custom logic
     *
     * @param str - query param
     * @return vet or null
     */
    private @Nullable
    Vet strToVet(String str) {
        Integer id;
        try {
            id = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
        return vets.findById(id);
    }

    /**
     * Check pet in visit belongs to owner, visit not cancelled
     *
     * @param owner .
     * @param visit .
     * @return access check result
     */
    private boolean checkAccess(Owner owner, Visit visit){
        try {
            Pet pet = pets.findById(visit.getPetId());
            return owner.getPetsInternal().contains(pet) && !visit.isAborted();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
