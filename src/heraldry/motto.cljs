(ns heraldry.motto
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.ribbon :as ribbon]))

(def tinctures-without-furs
  (-> tincture/choices
      (assoc 0 ["Metal"
                ["Argent" :argent]
                ["Or" :or]])
      (->> (keep-indexed #(when-not (= %1 2) %2)))
      vec))

(def default-options
  {:type {:type :choice
          :choices [["Motto" :heraldry.motto.type/motto]
                    ["Slogan" :heraldry.motto.type/slogan]]
          :ui {:label "Type"}}
   :origin (-> position/default-options
               (assoc-in [:point :choices] [["Top" :top]
                                            ["Bottom" :bottom]])
               (assoc-in [:point :default] :bottom)
               (assoc-in [:offset-x :min] -100)
               (assoc-in [:offset-x :max] 100)
               (assoc-in [:offset-y :min] -100)
               (assoc-in [:offset-y :max] 100)
               (dissoc :alignment)
               (assoc-in [:ui :label] "Origin"))
   :geometry (-> geometry/default-options
                 (select-keys [:size :ui])
                 (assoc-in [:size :min] 0.1)
                 (assoc-in [:size :max] 200)
                 (assoc-in [:size :default] 100))
   :ribbon-variant {:ui {:label "Ribbon"
                         :form-type :ribbon-reference-select}}
   :ribbon ribbon/default-options

   :tincture-foreground {:type :choice
                         :choices tinctures-without-furs
                         :default :argent
                         :ui {:label "Foreground"
                              :form-type :tincture-select}}

   :tincture-background {:type :choice
                         :choices (assoc tinctures-without-furs 0 ["Other / Metal"
                                                                   ["Darkened foreground" :none]
                                                                   ["Argent" :argent]
                                                                   ["Or" :or]])
                         :default :none
                         :ui {:label "Background"
                              :form-type :tincture-select}}

   :tincture-text {:type :choice
                   :choices tinctures-without-furs
                   :default :helmet-dark
                   :ui {:label "Text"
                        :form-type :tincture-select}}})

(defn options [data]
  (let [ribbon-variant? (:ribbon-variant data)
        motto-type (:type data)]
    (-> default-options
        (cond->
         ribbon-variant? (update :ribbon ribbon/options (:ribbon data))
         (not ribbon-variant?) (dissoc :ribbon)
         (= motto-type :heraldry.motto.type/slogan) (assoc-in [:origin :point :default] :top))
        (update :origin position/adjust-options))))

(defmethod interface/component-options :heraldry.component/motto [_path data]
  (options data))

