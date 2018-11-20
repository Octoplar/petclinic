package org.springframework.samples.petclinic.owner;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 */
@RunWith(SpringRunner.class)
@WebMvcTest(VisitController.class)
public class VisitControllerTests {

    private static final int TEST_PET_ID = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitRepository visits;

    @MockBean
    private VetRepository vets;

    @MockBean
    private OwnerRepository owners;

    @MockBean
    private PetRepository pets;

    @Before
    public void init() {
        given(this.pets.findById(TEST_PET_ID)).willReturn(new Pet());
    }

    @Test
    public void testInitNewVisitForm() throws Exception {
        mockMvc.perform(get("/owners/*/pets/{petId}/visits/new", TEST_PET_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }

    @Test
    public void testProcessNewVisitFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/*/pets/{petId}/visits/new", TEST_PET_ID)
            .param("petId", Integer.toString(TEST_PET_ID))
            .param("description", "Visit Description")
            .param("date", "2019-07-08")
            .param("vetString", "none")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

}
