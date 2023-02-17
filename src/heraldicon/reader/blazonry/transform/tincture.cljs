(ns heraldicon.reader.blazonry.transform.tincture
  (:require
   [clojure.string :as str]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first]]))

(def ^:private tincture-map
  (into {:tincture/PROPER :void}
        (map (fn [[key _]]
               [(keyword "tincture" (-> key name str/upper-case))
                key]))
        tincture/tincture-map))

(defmethod ast->hdn :tincture [[_ & nodes]]
  (let [ordinal (transform-first #{:ordinal} nodes)
        field (get-child #{:FIELD} nodes)
        same (get-child #{:SAME} nodes)]
    (cond
      field {::tincture-field-reference true}
      ordinal {::tincture-ordinal-reference ordinal}
      same (->> same
                rest
                (filter map?)
                first)
      :else (get tincture-map (ffirst nodes) :void))))

(defn find-tinctures [ast]
  (->> ast
       (tree-seq
        (fn [node]
          (and (vector? node)
               (-> node first keyword?)))
        rest)
       (keep (fn [ast]
               (let [kind (first ast)]
                 (if (= kind :SAME)
                   (second ast)
                   (get tincture-map kind)))))
       vec))
