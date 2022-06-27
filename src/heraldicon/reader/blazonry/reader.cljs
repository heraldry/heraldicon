(ns heraldicon.reader.blazonry.reader
  (:require
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.process.core :as process]
   [heraldicon.reader.blazonry.transform.core :as transform]
   [heraldicon.reader.blazonry.transform.tincture :as tincture]))

(defn read [data parser]
  (when data
    (let [ast (parser/parse data parser)
          tinctures (tincture/find-tinctures ast)]
      (-> ast
          transform/transform
          (process/process parser tinctures)))))
