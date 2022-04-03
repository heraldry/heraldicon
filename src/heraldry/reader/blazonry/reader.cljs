(ns heraldry.reader.blazonry.reader
  (:require
   [clojure.walk :as walk]
   [heraldry.reader.blazonry.parser :as parser]
   [heraldry.reader.blazonry.process :as process]
   [heraldry.reader.blazonry.transform :as transform]))

(defn read [data parser]
  (let [hdn (some-> data
                    (parser/parse parser)
                    transform/ast->hdn)]
    (->> hdn
         (walk/prewalk process/add-charge-group-defaults)
         (walk/postwalk process/process-ordinary-groups)
         (walk/postwalk (partial process/populate-charge-variants parser)))))
