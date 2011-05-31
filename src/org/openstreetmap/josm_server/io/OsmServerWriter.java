// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm_server.io;

//import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
//import static org.openstreetmap.josm.tools.I18n.trn;

//import java.util.ArrayList;
import java.util.Collection;
//import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
//import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.io.DiffResultEntry;
import org.openstreetmap.josm_server.gui.io.UploadStrategySpecification;
import org.openstreetmap.josm_server.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm_server.gui.progress.ProgressMonitor;
import org.openstreetmap.josm_server.io.auth.CredentialsManagerResponse;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * GWT
 *
 * TODO
 *  only one upload method implemented
 *
 * changelog
 *  - We have to send the result of the upload from server to client. Consequently
 *      we cannot update the OsmPrimitives in place, but have to send the diff results,
 *      so the client can update its dataset later. Therefore it does not suffice to
 *      save the processed primitives, but we need to keep more info.
 *  - No Authenticator is installed, to avoid problems where multiple client logins
 *      are mixed up.
 *      Instead, the credentials are always passed explicitly as parameter.
 */

/**
 * Class that uploads all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 */
public class OsmServerWriter {
    @SuppressWarnings("unused")
    static private final Logger logger = Logger.getLogger(OsmServerWriter.class.getName());

    /**
     * This list contains all successfully processed objects. The caller of
     * upload* has to check this after the call and update its dataset.
     *
     * If a server connection error occurs, this may contain fewer entries
     * than where passed in the list to upload*.
     */
//    private Collection<IPrimitive> processed;

    private Collection<? extends IPrimitive> primitives;

    private Map<PrimitiveId, DiffResultEntry> diffResults;

    private CredentialsManagerResponse credentials;

    private OsmApi api = OsmApi.getOsmApi();
    private boolean canceled = false;

    private static final int MSECS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MSECS_PER_MINUTE = MSECS_PER_SECOND * SECONDS_PER_MINUTE;

    long uploadStartTime;

    public String timeLeft(int progress, int list_size) {
        long now = System.currentTimeMillis();
        long elapsed = now - uploadStartTime;
        if (elapsed == 0) {
            elapsed = 1;
        }
        float uploads_per_ms = (float)progress / elapsed;
        float uploads_left = list_size - progress;
        int ms_left = (int)(uploads_left / uploads_per_ms);
        int minutes_left = ms_left / MSECS_PER_MINUTE;
        int seconds_left = (ms_left / MSECS_PER_SECOND) % SECONDS_PER_MINUTE ;
        String time_left_str = Integer.toString(minutes_left) + ":";
        if (seconds_left < 10) {
            time_left_str += "0";
        }
        time_left_str += Integer.toString(seconds_left);
        return time_left_str;
    }

//    /**
//     * Uploads the changes individually. Invokes one API call per uploaded primitmive.
//     *
//     * @param primitives the collection of primitives to upload
//     * @param progressMonitor the progress monitor
//     * @throws OsmTransferException thrown if an exception occurs
//     */
//    protected void uploadChangesIndividually(Collection<? extends IPrimitive> primitives, ProgressMonitor progressMonitor) throws OsmTransferException {
//        try {
//            progressMonitor.beginTask(tr("Starting to upload with one request per primitive ..."));
//            progressMonitor.setTicksCount(primitives.size());
//            uploadStartTime = System.currentTimeMillis();
//            for (IPrimitive osm : primitives) {
//                int progress = progressMonitor.getTicks();
//                String time_left_str = timeLeft(progress, primitives.size());
//                String msg = "";
//                switch(OsmPrimitiveType.from(osm)) {
//                case NODE: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading node ''{4}'' (id: {5})"); break;
//                case WAY: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading way ''{4}'' (id: {5})"); break;
//                case RELATION: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading relation ''{4}'' (id: {5})"); break;
//                }
//                progressMonitor.subTask(
//                        tr(msg,
//                                Math.round(100.0*progress/primitives.size()),
//                                progress,
//                                primitives.size(),
//                                time_left_str,
//                                osm.getName() == null ? osm.getId() : osm.getName(),
//                                        osm.getId()));
//                makeApiRequest(osm,progressMonitor);
//                processed.add(osm);
//                progressMonitor.worked(1);
//            }
//        } catch(OsmTransferException e) {
//            throw e;
//        } catch(Exception e) {
//            throw new OsmTransferException(e);
//        } finally {
//            progressMonitor.finishTask();
//        }
//    }

    /**
     * Upload all changes in one diff upload
     *
     * @param primitives the collection of primitives to upload
     * @param progressMonitor  the progress monitor
     * @throws OsmTransferException thrown if an exception occurs
     */
    protected void uploadChangesAsDiffUpload(Collection<? extends IPrimitive> primitives, ProgressMonitor progressMonitor) throws OsmTransferException {
        this.primitives = primitives;
        try {
            progressMonitor.beginTask(tr("Starting to upload in one request ..."));
            diffResults = api.uploadDiff(primitives, credentials, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        } catch(OsmTransferException e) {
            throw e;
        } finally {
            progressMonitor.finishTask();
        }
    }

//    /**
//     * Upload all changes in one diff upload
//     *
//     * @param primitives the collection of primitives to upload
//     * @param progressMonitor  the progress monitor
//     * @param chunkSize the size of the individual upload chunks. > 0 required.
//     * @throws IllegalArgumentException thrown if chunkSize <= 0
//     * @throws OsmTransferException thrown if an exception occurs
//     */
//    protected void uploadChangesInChunks(Collection<? extends IPrimitive> primitives, ProgressMonitor progressMonitor, int chunkSize) throws OsmTransferException, IllegalArgumentException {
//        if (chunkSize <=0)
//            throw new IllegalArgumentException(tr("Value >0 expected for parameter ''{0}'', got {1}", "chunkSize", chunkSize));
//        try {
//            progressMonitor.beginTask(tr("Starting to upload in chunks..."));
//            List<IPrimitive> chunk = new ArrayList<IPrimitive>(chunkSize);
//            Iterator<? extends IPrimitive> it = primitives.iterator();
//            int numChunks = (int)Math.ceil((double)primitives.size() / (double)chunkSize);
//            int i= 0;
//            while(it.hasNext()) {
//                i++;
//                if (canceled) return;
//                int j = 0;
//                chunk.clear();
//                while(it.hasNext() && j < chunkSize) {
//                    if (canceled) return;
//                    j++;
//                    chunk.add(it.next());
//                }
//                progressMonitor.setCustomText(
//                        trn("({0}/{1}) Uploading {2} object...",
//                                "({0}/{1}) Uploading {2} objects...",
//                                chunk.size(), i, numChunks, chunk.size()));
//                processed.addAll(api.uploadDiff(chunk, credentials, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false)));
//            }
//        } catch(OsmTransferException e) {
//            throw e;
//        } finally {
//            progressMonitor.finishTask();
//        }
//    }

    /**
     * Send the dataset to the server.
     *
     * @param strategy the upload strategy. Must not be null.
     * @param primitives list of objects to send
     * @param changeset the changeset the data is uploaded to. Must not be null.
     * @param monitor the progress monitor. If null, assumes {@see NullProgressMonitor#INSTANCE}
     * @throws IllegalArgumentException thrown if changeset is null
     * @throws IllegalArgumentException thrown if strategy is null
     * @throws OsmTransferException thrown if something goes wrong
     */
    public void uploadOsm(UploadStrategySpecification strategy, Collection<? extends IPrimitive> primitives, Changeset changeset, CredentialsManagerResponse credentials, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(changeset, "changeset");
//        processed = new LinkedList<IPrimitive>();
        this.credentials = credentials;
        monitor = monitor == null ? NullProgressMonitor.INSTANCE : monitor;
        monitor.beginTask(tr("Uploading data ..."));
        try {
            api.initialize(monitor);
            // check whether we can use diff upload
            if (changeset.getId() == 0) {
                api.openChangeset(changeset, credentials, monitor.createSubTaskMonitor(0, false));
            } else {
                api.updateChangeset(changeset, credentials, monitor.createSubTaskMonitor(0, false));
            }
            api.setChangeset(changeset);
            switch(strategy.getStrategy()) {
            case SINGLE_REQUEST_STRATEGY:
                uploadChangesAsDiffUpload(primitives,monitor.createSubTaskMonitor(0,false));
                break;
//            case INDIVIDUAL_OBJECTS_STRATEGY:
//                uploadChangesIndividually(primitives,monitor.createSubTaskMonitor(0,false));
//                break;
//            case CHUNKED_DATASET_STRATEGY:
//                uploadChangesInChunks(primitives,monitor.createSubTaskMonitor(0,false), strategy.getChunkSize());
//                break;
            }
        } catch(OsmTransferException e) {
            throw e;
        } finally {
            monitor.finishTask();
            api.setChangeset(null);
        }
    }

    void makeApiRequest(IPrimitive osm, ProgressMonitor progressMonitor) throws OsmTransferException {
        if (osm.isDeleted()) {
            api.deletePrimitive(osm, credentials, progressMonitor);
        } else if (osm.isNew()) {
            api.createPrimitive(osm, credentials, progressMonitor);
        } else {
            api.modifyPrimitive(osm, credentials, progressMonitor);
        }
    }

    public void cancel() {
        this.canceled = true;
        if (api != null) {
            api.cancel();
        }
    }

    /**
     * Replies the collection of successfully processed primitives
     *
     * @return the collection of successfully processed primitives
     */
    public Collection<IPrimitive> getProcessedPrimitives() {
        List<IPrimitive> processed = new LinkedList<IPrimitive>();
        for (IPrimitive osm : primitives) {
            if (diffResults.containsKey(osm)) {
                processed.add(osm);
            }
        }
        return processed;
    }

    public Map<PrimitiveId, DiffResultEntry> getDiffResults() {
        return diffResults;
    }

}
