(ns heraldicon.reader.blazonry.reader
  (:require
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.process :as process]
   [heraldicon.reader.blazonry.transform :as transform]))

(defn read [data parser]
  (let [ast (some-> data
                    (parser/parse parser))
        tinctures (transform/find-tinctures ast)]
    (some-> ast
            transform/ast->hdn
            process/process-charge-groups
            process/process-ordinary-groups
            (process/process-charge-references parser)
            (process/process-tincture-references tinctures))))