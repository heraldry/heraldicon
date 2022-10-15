(ns heraldicon.reader.blazonry.process.core
  (:require
   [heraldicon.reader.blazonry.process.charge :as charge]
   [heraldicon.reader.blazonry.process.charge-group :as charge-group]
   [heraldicon.reader.blazonry.process.ordinary :as ordinary]
   [heraldicon.reader.blazonry.process.tincture :as tincture]))

(defn process [hdn parser tinctures]
  (some-> hdn
          ordinary/process
          charge-group/process
          (charge/process parser)
          (tincture/process tinctures)))
