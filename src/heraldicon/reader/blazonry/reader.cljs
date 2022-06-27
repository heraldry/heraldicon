(ns heraldicon.reader.blazonry.reader
  (:require
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.process :as process]
   [heraldicon.reader.blazonry.transform :as transform]
   [heraldicon.reader.blazonry.transform.tincture :as tincture]))

(defn read [data parser]
  (let [ast (some-> data
                    (parser/parse parser))
        tinctures (tincture/find-tinctures ast)]
    (some-> ast
            transform/transform
            process/process-charge-groups
            process/process-ordinary-groups
            (process/process-charge-references parser)
            (process/process-tincture-references tinctures))))
