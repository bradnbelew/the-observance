// Database type contract for The Observance dashboard.
//
// This is the single source of truth for table/column shapes used across every
// Supabase client, query, and server action. It mirrors the SQL migrations in
// supabase/migrations/ (0001_init.sql, 0002_seed.sql) exactly. Keep them in
// lockstep.
//
// Generated-style hand-authored Database type (compatible with the shape
// @supabase/supabase-js expects from `Database`). Tables are read/written by the
// Minecraft plugin; the dashboard mostly READS and writes a few control rows.

export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[];

// String-literal unions for the CHECK-constrained text columns.
// (Postgres CHECK constraints, not enums — kept in sync with 0001_init.sql.)
export type BeatStatus = "pending" | "approved" | "skipped" | "fired";
export type EventLevel = "info" | "warn" | "error";

export interface Database {
  public: {
    Tables: {
      players: {
        Row: {
          id: string;
          mc_uuid: string;
          name: string;
          first_seen: string;
          last_seen: string;
        };
        Insert: {
          id?: string;
          mc_uuid: string;
          name: string;
          first_seen?: string;
          last_seen?: string;
        };
        Update: {
          id?: string;
          mc_uuid?: string;
          name?: string;
          first_seen?: string;
          last_seen?: string;
        };
        Relationships: [];
      };
      dossiers: {
        Row: {
          player_id: string;
          solo_ratio: number;
          deaths: number;
          hoard_summary: string | null;
          group_distance: number | null;
          chat_sentiment: number | null;
          blocks_mined: number;
          updated_at: string;
        };
        Insert: {
          player_id: string;
          solo_ratio?: number;
          deaths?: number;
          hoard_summary?: string | null;
          group_distance?: number | null;
          chat_sentiment?: number | null;
          blocks_mined?: number;
          updated_at?: string;
        };
        Update: {
          player_id?: string;
          solo_ratio?: number;
          deaths?: number;
          hoard_summary?: string | null;
          group_distance?: number | null;
          chat_sentiment?: number | null;
          blocks_mined?: number;
          updated_at?: string;
        };
        Relationships: [
          {
            foreignKeyName: "dossiers_player_id_fkey";
            columns: ["player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      custom_compliance: {
        Row: {
          id: number;
          player_id: string | null;
          custom_key: string;
          last_observed: string | null;
          violation_count: number;
          status: string;
        };
        Insert: {
          id?: number;
          player_id?: string | null;
          custom_key: string;
          last_observed?: string | null;
          violation_count?: number;
          status?: string;
        };
        Update: {
          id?: number;
          player_id?: string | null;
          custom_key?: string;
          last_observed?: string | null;
          violation_count?: number;
          status?: string;
        };
        Relationships: [
          {
            foreignKeyName: "custom_compliance_player_id_fkey";
            columns: ["player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      heatmap_cells: {
        Row: {
          id: number;
          world: string;
          cell_x: number;
          cell_z: number;
          visits: number;
          updated_at: string;
        };
        Insert: {
          id?: number;
          world: string;
          cell_x: number;
          cell_z: number;
          visits?: number;
          updated_at?: string;
        };
        Update: {
          id?: number;
          world?: string;
          cell_x?: number;
          cell_z?: number;
          visits?: number;
          updated_at?: string;
        };
        Relationships: [];
      };
      bases: {
        Row: {
          id: number;
          owner_player_id: string | null;
          world: string | null;
          x: number | null;
          z: number | null;
          confidence: number;
          updated_at: string;
        };
        Insert: {
          id?: number;
          owner_player_id?: string | null;
          world?: string | null;
          x?: number | null;
          z?: number | null;
          confidence?: number;
          updated_at?: string;
        };
        Update: {
          id?: number;
          owner_player_id?: string | null;
          world?: string | null;
          x?: number | null;
          z?: number | null;
          confidence?: number;
          updated_at?: string;
        };
        Relationships: [
          {
            foreignKeyName: "bases_owner_player_id_fkey";
            columns: ["owner_player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      whisper_budgets: {
        Row: {
          id: number;
          player_id: string | null;
          act: number;
          budget: number;
          spent: number;
          earned: number;
        };
        Insert: {
          id?: number;
          player_id?: string | null;
          act: number;
          budget?: number;
          spent?: number;
          earned?: number;
        };
        Update: {
          id?: number;
          player_id?: string | null;
          act?: number;
          budget?: number;
          spent?: number;
          earned?: number;
        };
        Relationships: [
          {
            foreignKeyName: "whisper_budgets_player_id_fkey";
            columns: ["player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      whisper_events: {
        Row: {
          id: number;
          player_id: string | null;
          puzzle_key: string | null;
          tier: number | null;
          created_at: string;
        };
        Insert: {
          id?: number;
          player_id?: string | null;
          puzzle_key?: string | null;
          tier?: number | null;
          created_at?: string;
        };
        Update: {
          id?: number;
          player_id?: string | null;
          puzzle_key?: string | null;
          tier?: number | null;
          created_at?: string;
        };
        Relationships: [
          {
            foreignKeyName: "whisper_events_player_id_fkey";
            columns: ["player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      bond_ledger: {
        Row: {
          player_id: string;
          bond_points: number;
          updated_at: string;
        };
        Insert: {
          player_id: string;
          bond_points?: number;
          updated_at?: string;
        };
        Update: {
          player_id?: string;
          bond_points?: number;
          updated_at?: string;
        };
        Relationships: [
          {
            foreignKeyName: "bond_ledger_player_id_fkey";
            columns: ["player_id"];
            referencedRelation: "players";
            referencedColumns: ["id"];
          },
        ];
      };
      arc_state: {
        Row: {
          id: number;
          current_act: number;
          gates: Json;
          flags: Json;
          updated_at: string;
        };
        Insert: {
          id?: number;
          current_act?: number;
          gates?: Json;
          flags?: Json;
          updated_at?: string;
        };
        Update: {
          id?: number;
          current_act?: number;
          gates?: Json;
          flags?: Json;
          updated_at?: string;
        };
        Relationships: [];
      };
      beat_queue: {
        Row: {
          id: number;
          type: string;
          target: string | null;
          payload: Json;
          status: BeatStatus;
          created_at: string;
          decided_at: string | null;
        };
        Insert: {
          id?: number;
          type: string;
          target?: string | null;
          payload?: Json;
          status?: BeatStatus;
          created_at?: string;
          decided_at?: string | null;
        };
        Update: {
          id?: number;
          type?: string;
          target?: string | null;
          payload?: Json;
          status?: BeatStatus;
          created_at?: string;
          decided_at?: string | null;
        };
        Relationships: [];
      };
      event_log: {
        Row: {
          id: number;
          level: EventLevel;
          source: string | null;
          message: string | null;
          created_at: string;
        };
        Insert: {
          id?: number;
          level?: EventLevel;
          source?: string | null;
          message?: string | null;
          created_at?: string;
        };
        Update: {
          id?: number;
          level?: EventLevel;
          source?: string | null;
          message?: string | null;
          created_at?: string;
        };
        Relationships: [];
      };
      settings: {
        Row: {
          key: string;
          value: Json;
          updated_at: string;
        };
        Insert: {
          key: string;
          value: Json;
          updated_at?: string;
        };
        Update: {
          key?: string;
          value?: Json;
          updated_at?: string;
        };
        Relationships: [];
      };
    };
    Views: {
      v_health: {
        Row: {
          last_beat_at: string | null;
          info_24h: number;
          warn_24h: number;
          error_24h: number;
          watcher_sleep: Json;
          api_status: Json;
          whisper_status: Json;
        };
        Relationships: [];
      };
      v_heatmap: {
        Row: {
          world: string;
          cell_x: number;
          cell_z: number;
          visits: number;
        };
        Relationships: [];
      };
      v_compliance_counts: {
        Row: {
          total_records: number;
          total_flags: number;
        };
        Relationships: [];
      };
      // Reconciling views (0005_reconcile_tracker_views.sql). They reshape the
      // plugin's flat, mc_uuid-keyed dossiers / custom_compliance rows back into
      // the SAME Row shapes the dossiers / custom_compliance tables declare above,
      // synthesizing player_id via players.mc_uuid. SPOILER-RICH — read only by
      // the service_role admin client (no anon grant). The author page reads
      // these instead of the raw tables so plugin rows join and render.
      v_dossiers: {
        Row: {
          player_id: string;
          solo_ratio: number;
          deaths: number;
          hoard_summary: string | null;
          group_distance: number | null;
          chat_sentiment: number | null;
          blocks_mined: number;
          updated_at: string;
        };
        Relationships: [];
      };
      v_custom_compliance: {
        Row: {
          id: number;
          player_id: string | null;
          custom_key: string;
          last_observed: string | null;
          violation_count: number;
          status: string;
        };
        Relationships: [];
      };
    };
    Functions: Record<string, never>;
    Enums: Record<string, never>;
    CompositeTypes: Record<string, never>;
  };
}

// Convenience row aliases for page/query agents.
type PublicSchema = Database["public"];

export type Tables<T extends keyof PublicSchema["Tables"]> =
  PublicSchema["Tables"][T]["Row"];
export type TablesInsert<T extends keyof PublicSchema["Tables"]> =
  PublicSchema["Tables"][T]["Insert"];
export type TablesUpdate<T extends keyof PublicSchema["Tables"]> =
  PublicSchema["Tables"][T]["Update"];
export type Views<T extends keyof PublicSchema["Views"]> =
  PublicSchema["Views"][T]["Row"];

// ---------------------------------------------------------------------------
// Convenient row aliases (the shapes the app reaches for most often).
// ---------------------------------------------------------------------------

// Table rows
export type Player = Tables<"players">;
export type Dossier = Tables<"dossiers">;
export type CustomCompliance = Tables<"custom_compliance">;
export type HeatmapCell = Tables<"heatmap_cells">;
export type Base = Tables<"bases">;
export type WhisperBudget = Tables<"whisper_budgets">;
export type WhisperEvent = Tables<"whisper_events">;
export type BondLedger = Tables<"bond_ledger">;
export type ArcState = Tables<"arc_state">;
export type Beat = Tables<"beat_queue">;
export type EventLogRow = Tables<"event_log">;
export type Setting = Tables<"settings">;

// Insert aliases for the rows the dashboard writes
export type BeatInsert = TablesInsert<"beat_queue">;
export type WhisperBudgetInsert = TablesInsert<"whisper_budgets">;
export type SettingInsert = TablesInsert<"settings">;

// Update aliases for the four control-surface writes:
//   * approve/skip a beat            -> BeatUpdate
//   * edit a whisper budget          -> WhisperBudgetUpdate
//   * toggle watcher_sleep / status  -> SettingUpdate
//   * advance the arc act            -> ArcStateUpdate
export type BeatUpdate = TablesUpdate<"beat_queue">;
export type WhisperBudgetUpdate = TablesUpdate<"whisper_budgets">;
export type SettingUpdate = TablesUpdate<"settings">;
export type ArcStateUpdate = TablesUpdate<"arc_state">;

// Spoiler-free view rows (all the public/status mode may read)
export type HealthView = Views<"v_health">;
export type HeatmapView = Views<"v_heatmap">;
export type ComplianceCountsView = Views<"v_compliance_counts">;
