(ns heraldicon.reader.blazonry.process.core
  (:require
   [heraldicon.reader.blazonry.process.charge :as process.charge]
   [heraldicon.reader.blazonry.process.charge-group :as charge-group]
   [heraldicon.reader.blazonry.process.tincture :as tincture]))

(defn process [hdn parser tinctures]
  (some-> hdn
          charge-group/process
          (process.charge/process parser)
          (tincture/process tinctures)))
