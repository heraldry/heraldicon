(ns heraldicon.reader.blazonry.transform.core
  (:require
   [heraldicon.reader.blazonry.transform.amount]
   [heraldicon.reader.blazonry.transform.charge]
   [heraldicon.reader.blazonry.transform.charge-group]
   [heraldicon.reader.blazonry.transform.cottising]
   [heraldicon.reader.blazonry.transform.field.core]
   [heraldicon.reader.blazonry.transform.field.partition]
   [heraldicon.reader.blazonry.transform.field.partition.field]
   [heraldicon.reader.blazonry.transform.field.plain]
   [heraldicon.reader.blazonry.transform.fimbriation]
   [heraldicon.reader.blazonry.transform.layout]
   [heraldicon.reader.blazonry.transform.line]
   [heraldicon.reader.blazonry.transform.ordinal]
   [heraldicon.reader.blazonry.transform.ordinary]
   [heraldicon.reader.blazonry.transform.ordinary-group]
   [heraldicon.reader.blazonry.transform.semy]
   [heraldicon.reader.blazonry.transform.shared :as shared]
   [heraldicon.reader.blazonry.transform.tincture]
   [heraldicon.reader.blazonry.transform.tincture-modifier]))

(defn transform [ast]
  (shared/ast->hdn ast))
