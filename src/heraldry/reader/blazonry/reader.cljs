(ns heraldry.reader.blazonry.reader
  (:require
   [heraldry.reader.blazonry.parser :as parser]
   [heraldry.reader.blazonry.process :as process]
   [heraldry.reader.blazonry.transform :as transform]))

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
