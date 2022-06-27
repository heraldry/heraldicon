(ns heraldicon.reader.blazonry.transform.core
  (:require
   [heraldicon.reader.blazonry.transform.amount] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.charge] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.charge-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.cottising] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.plain] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.fimbriation] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.layout] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.line] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinal] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.semy] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :as shared]
   [heraldicon.reader.blazonry.transform.tincture] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.tincture-modifier] ;; needed for side effects
   ))

(defn transform [ast]
  (shared/ast->hdn ast))
