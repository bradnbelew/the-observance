package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Exact server-only response from observance_record_arg_event. */
public final class ArgEventRpcRow {
    public String status;
    public Boolean created;

    @SerializedName("event_id")
    public String eventId;

    @SerializedName("missing_prerequisites")
    public List<String> missingPrerequisites;

    public ArgEventRpcRow() { }
}
