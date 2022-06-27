(ns heraldicon.reader.blazonry.reader
  (:require
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.process :as process]
   [heraldicon.reader.blazonry.transform] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :as transform.shared]
   [heraldicon.reader.blazonry.transform.tincture :as tincture]))

(defn read [data parser]
  (let [ast (some-> data
                    (parser/parse parser))
        tinctures (tincture/find-tinctures ast)]
    (some-> ast
            transform.shared/ast->hdn
            process/process-charge-groups
            process/process-ordinary-groups
            (process/process-charge-references parser)
            (process/process-tincture-references tinctures))))
