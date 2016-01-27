package wde.inference;

import wde.obs.ObsSet;
import wde.obs.Observation;
import wde.util.DatabaseArrayParser;
import wde.util.QualityCheckFlagUtil;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InferenceUnitTests {
    
    /*
    obstypeid | sourceid | sensorid |       obstime       |          recvtime          | latitude | longitude | elevation |      value       | confvalue |        qchcharflag        
-----------+----------+----------+---------------------+----------------------------+----------+-----------+-----------+------------------+-----------+---------------------------
       581 |        1 |   409021 | 2014-05-02 16:59:31 | 2014-05-02 17:10:00.292+00 | 42339130 | -82996280 |       483 |               68 |         1 | {P,-,P,P,P,/,P,-,-,-,/,/}
       581 |        1 |   409021 | 2014-05-02 16:57:19 | 2014-05-02 17:10:00.292+00 | 42342590 | -82979820 |       467 |               67 |         1 | {P,-,P,P,P,/,P,-,-,-,/,/}
       581 |        1 |   409021 | 2014-05-02 16:57:13 | 2014-05-02 17:10:00.292+00 | 42342860 | -82979220 |       461 |               68 |         1 | {P,-,P,P,P,/,P,-,-,-,/,/}
       575 |        1 |   409019 | 2014-05-02 16:59:31 | 2014-05-02 17:10:00.447+00 | 42339130 | -82996280 |       483 | 6.94444444444444 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:59:28 | 2014-05-02 17:10:00.447+00 | 42339330 | -82996170 |       485 | 6.88888888888889 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:59:13 | 2014-05-02 17:10:00.447+00 | 42338960 | -82994470 |       473 | 6.83333333333333 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:59:09 | 2014-05-02 17:10:00.447+00 | 42338920 | -82993900 |       481 | 6.88888888888889 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:59:01 | 2014-05-02 17:10:00.447+00 | 42339260 | -82992820 |       469 | 6.94444444444444 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:58 | 2014-05-02 17:10:00.447+00 | 42339400 | -82992420 |       469 |                7 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:48 | 2014-05-02 17:10:00.447+00 | 42339870 | -82991050 |       476 | 6.94444444444444 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:35 | 2014-05-02 17:10:00.447+00 | 42340250 | -82989370 |       477 | 6.88888888888889 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:28 | 2014-05-02 17:10:00.447+00 | 42340530 | -82988430 |       491 | 6.94444444444444 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:19 | 2014-05-02 17:10:00.447+00 | 42340860 | -82987290 |       499 |                7 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:06 | 2014-05-02 17:10:00.447+00 | 42341200 | -82985720 |       506 | 7.05555555555556 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:58:03 | 2014-05-02 17:10:00.447+00 | 42341300 | -82985340 |       499 | 7.11111111111111 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:50 | 2014-05-02 17:10:00.447+00 | 42341660 | -82983750 |       506 | 7.05555555555556 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:45 | 2014-05-02 17:10:00.447+00 | 42341850 | -82982950 |       509 | 7.11111111111111 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:44 | 2014-05-02 17:10:00.447+00 | 42341880 | -82982810 |       510 | 7.05555555555556 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:35 | 2014-05-02 17:10:00.447+00 | 42342150 | -82981660 |       487 |                7 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:25 | 2014-05-02 17:10:00.447+00 | 42342430 | -82980490 |       477 | 7.05555555555556 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:22 | 2014-05-02 17:10:00.447+00 | 42342500 | -82980140 |       472 | 7.11111111111111 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:18 | 2014-05-02 17:10:00.447+00 | 42342620 | -82979720 |       464 | 7.16666666666667 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
       575 |        1 |   409019 | 2014-05-02 16:57:13 | 2014-05-02 17:10:00.447+00 | 42342860 | -82979220 |       461 | 7.27777777777778 |         1 | {P,-,P,P,P,/,P,-,-,/,/,/}
     51138 |        1 |   409018 | 2014-05-02 16:59:40 | 2014-05-02 17:10:00.297+00 | 42338240 | -82996730 |       483 | 23.0555555555556 |         1 | {P,-,P,P,P,-,P,-,-,/,/,/}
     51138 |        1 |   409018 | 2014-05-02 16:59:39 | 2014-05-02 17:10:00.297+00 | 42338350 | -82996660 |       483 | 23.3333333333333 |         1 | {P,-,P,P,P,-,P,-,-,/,/,/}
    (25 rows)
     */

    private final Object[][] observations = new Object[][]{
            new Object[]{581, 1, 409021, "2014-05-02 16:59:31", "2014-05-02 17:10:00.292+00", 42339130, -82996280, 483, 68, 1, "{P,-,P,P,P,/,P,-,-,-,/,/}"},
            new Object[]{581, 1, 409021, "2014-05-02 16:57:19", "2014-05-02 17:10:00.292+00", 42342590, -82979820, 467, 67, 1, "{P,-,P,P,P,/,P,-,-,-,/,/}"},
            new Object[]{581, 1, 409021, "2014-05-02 16:57:13", "2014-05-02 17:10:00.292+00", 42342860, -82979220, 461, 68, 1, "{P,-,P,P,P,/,P,-,-,-,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:59:31", "2014-05-02 17:10:00.447+00", 42339130, -82996280, 483, 6.94444444444444, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:59:28", "2014-05-02 17:10:00.447+00", 42339330, -82996170, 485, 6.88888888888889, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:59:13", "2014-05-02 17:10:00.447+00", 42338960, -82994470, 473, 6.83333333333333, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:59:09", "2014-05-02 17:10:00.447+00", 42338920, -82993900, 481, 6.88888888888889, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:59:01", "2014-05-02 17:10:00.447+00", 42339260, -82992820, 469, 6.94444444444444, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:58", "2014-05-02 17:10:00.447+00", 42339400, -82992420, 469, 7, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:48", "2014-05-02 17:10:00.447+00", 42339870, -82991050, 476, 6.94444444444444, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:35", "2014-05-02 17:10:00.447+00", 42340250, -82989370, 477, 6.88888888888889, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:28", "2014-05-02 17:10:00.447+00", 42340530, -82988430, 491, 6.94444444444444, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:19", "2014-05-02 17:10:00.447+00", 42340860, -82987290, 499, 7, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:06", "2014-05-02 17:10:00.447+00", 42341200, -82985720, 506, 7.05555555555556, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:58:03", "2014-05-02 17:10:00.447+00", 42341300, -82985340, 499, 7.11111111111111, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:50", "2014-05-02 17:10:00.447+00", 42341660, -82983750, 506, 7.05555555555556, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:45", "2014-05-02 17:10:00.447+00", 42341850, -82982950, 509, 7.11111111111111, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:44", "2014-05-02 17:10:00.447+00", 42341880, -82982810, 510, 7.05555555555556, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:35", "2014-05-02 17:10:00.447+00", 42342150, -82981660, 487, 7, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:25", "2014-05-02 17:10:00.447+00", 42342430, -82980490, 477, 7.05555555555556, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:22", "2014-05-02 17:10:00.447+00", 42342500, -82980140, 472, 7.11111111111111, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:18", "2014-05-02 17:10:00.447+00", 42342620, -82979720, 464, 7.16666666666667, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{575, 1, 409019, "2014-05-02 16:57:13", "2014-05-02 17:10:00.447+00", 42342860, -82979220, 461, 7.27777777777778, 1, "{P,-,P,P,P,/,P,-,-,/,/,/}"},
            new Object[]{51138, 1, 409018, "2014-05-02 16:59:40", "2014-05-02 17:10:00.297+00", 42338240, -82996730, 483, 23.0555555555556, 1, "{P,-,P,P,P,-,P,-,-,/,/,/}"},
            new Object[]{51138, 1, 409018, "2014-05-02 16:59:39", "2014-05-02 17:10:00.297+00", 42338350, -82996660, 483, 23.3333333333333, 1, "{P,-,P,P,P,-,P,-,-,/,/,/}"}
    };

    public InferenceManager newInferenceManager() {
        return new InferenceManager(false);
    }

    public InferenceManager getInferenceManagerSingleton() {
        return InferenceManager.getInstance();
    }

    public List<String> parseQualtiyCheckFlags(String flags) throws Exception {
        DatabaseArrayParser dap = new DatabaseArrayParser();

        return dap.postgresROW2StringList(flags);
    }

    public Observation buildObservationFromArray(Object[] array) throws Exception {
        return new Observation(
                        (int) array[0],
                        (int) array[1],
                        (int) array[2],
                        Date.valueOf(array[3].toString()).getTime(),
                        Date.valueOf(array[4].toString()).getTime(),
                        (int) array[5],
                        (int) array[6],
                        (short) array[7],
                        (double) array[8],
                        QualityCheckFlagUtil.getPassingQcFlags(),
                        (float) array[10]
        );
    }

    public Observation[] buildObservationsFromArray(Object[][] array) throws Exception {
        ArrayList<Observation> obsList = new ArrayList<>();

        for (Object[] row : array) {
            obsList.add(new Observation(
                            (int) row[0],
                            (int) row[1],
                            (int) row[2],
                            Date.valueOf(row[3].toString()).getTime(),
                            Date.valueOf(row[4].toString()).getTime(),
                            (int) row[5],
                            (int) row[6],
                            (short) row[7],
                            (double) row[8],
                            QualityCheckFlagUtil.getPassingQcFlags(),
                            (float) row[10])
            );
        }

        return (Observation[]) obsList.toArray();
    }

    public Integer[] getUniqueObstypes(Object[][] observations) {
        Set<Integer> obstypeSet = new HashSet<>();
        for (Object[] obs : observations) {
            obstypeSet.add((int) obs[0]);
        }

        return obstypeSet.toArray(new Integer[]{});
    }

    public void testInferenceManager() throws Exception {
        InferenceManager inferenceManager = newInferenceManager();

        Integer[] uniqueObstypeIds = getUniqueObstypes(observations);
        for (int i = 0; i < uniqueObstypeIds.length; ++i) {
            List<Observation> observationList = new ArrayList<Observation>();

            ObsSet obsSet = new ObsSet(uniqueObstypeIds[i]);
            for (int j = 0; j < observations.length; ++j) {
                if ((int) observations[j][0] == uniqueObstypeIds[i]) {
                    observationList.add(buildObservationFromArray(observations[j]));
                }
            }

            inferenceManager.run(obsSet);
        }
    }

    public static void main(String[] args) throws Exception {

        InferenceUnitTests unitTests = new InferenceUnitTests();

        unitTests.testInferenceManager();
    }

}
