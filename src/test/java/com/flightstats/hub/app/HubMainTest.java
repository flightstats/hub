package com.flightstats.hub.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
public class HubMainTest {

    @Mock
    private HubMain hubMain;

    @Test
    public void shouldThrowUnsupportedOperationExceptionWhenArgsAreEmpty() {
        String[] args = {};
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            HubMain.main(args);
        });

        assertEquals("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'", exception.getMessage());
    }

  /*  @Test
    public void testUnsafePathArgument() {
        String unsafePath = "../../../../../../etc/passwd";
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            hubMain.main(new String[]{unsafePath});
        });
        assertEquals("HubMain requires a valid property filename", exception.getMessage());

    }*/

}
