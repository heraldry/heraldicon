(ns heraldry.coat-of-arms.charge.options
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.type.annulet :as annulet]
   [heraldry.coat-of-arms.charge.type.billet :as billet]
   [heraldry.coat-of-arms.charge.type.crescent :as crescent]
   [heraldry.coat-of-arms.charge.type.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.charge.type.fusil :as fusil]
   [heraldry.coat-of-arms.charge.type.lozenge :as lozenge]
   [heraldry.coat-of-arms.charge.type.mascle :as mascle]
   [heraldry.coat-of-arms.charge.type.roundel :as roundel]
   [heraldry.coat-of-arms.charge.type.rustre :as rustre]
   [heraldry.coat-of-arms.escutcheon :as root-escutcheon]
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def charges
  [roundel/charge-type
   annulet/charge-type
   billet/charge-type
   escutcheon/charge-type
   lozenge/charge-type
   fusil/charge-type
   mascle/charge-type
   rustre/charge-type
   crescent/charge-type])

(def choices
  (->> charges
       (map (fn [key]
              [(charge-interface/display-name key) key]))))

(def choice-map
  (util/choices->map choices))

(def default-options
  {:type {:type :choice
          :choices choices
          :ui {:label strings/type
               :form-type :charge-type-select}}
   :origin (-> position/default-options
               (assoc :alignment nil)
               (assoc-in [:ui :label] strings/origin))
   :anchor (-> position/anchor-default-options
               (assoc-in [:point :default] :angle)
               (update-in [:point :choices] (fn [choices]
                                              (-> choices
                                                  drop-last
                                                  (conj (last choices))
                                                  vec)))
               (assoc :alignment nil)
               (assoc-in [:angle :min] -180)
               (assoc-in [:angle :max] 180)
               (assoc-in [:angle :default] 0)
               (assoc-in [:ui :label] strings/anchor))
   :geometry geometry/default-options
   :escutcheon {:type :choice
                :choices (assoc-in (vec root-escutcheon/choices) [0 0] {:en "Root"
                                                                        :de "Ursprung"})
                :default :none
                :ui {:label strings/escutcheon
                     :form-type :escutcheon-select}}
   :fimbriation (-> line/default-options
                    :fimbriation
                    (dissoc :alignment)
                    (assoc-in [:corner :default] :round)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-1 :max] 50)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-2 :max] 50)
                    (assoc-in [:thickness-2 :default] 10))
   :tincture {:eyed {:type :choice
                     :choices tincture/choices
                     :default :argent
                     :ui {:label {:en "Eyed"
                                  :de "Augen"}}}
              :toothed {:type :choice
                        :choices tincture/choices
                        :default :argent
                        :ui {:label {:en "Toothed"
                                     :de "Zähne"}}}
              :shadow {:type :range
                       :min 0
                       :max 1
                       :default 1
                       :ui {:label strings/shadow
                            :step 0.01}}
              :highlight {:type :range
                          :min 0
                          :max 1
                          :default 1
                          :ui {:label strings/highlight
                               :step 0.01}}
              :ui {:label {:en "Tinctures"
                           :de "Tinkturen"}
                   :form-type :tincture-modifiers}}
   :outline-mode {:type :choice
                  :choices [[{:en "Keep"
                              :de "Anzeigen"} :keep]
                            ["Transparent" :transparent]
                            [{:en "Primary"
                              :de "Primär"} :primary]
                            [{:en "Remove"
                              :de "Entfernen"} :remove]]
                  :default :keep
                  :ui {:label strings/outline}}
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}
   :vertical-mask {:type :range
                   :default 0
                   :min -100
                   :max 100
                   :ui {:label {:en "Vertical mask"
                                :de "Vertikale Maske"}
                        :step 1}}
   :ignore-layer-separator? {:type :boolean
                             :default false
                             :ui {:label {:en "Ignore layer separator"
                                          :de "Ebenentrenner ignorieren"}
                                  :tooltip "If the charge contains a layer separator for the shield, then this can disable it."}}})

(defn options [charge & {:keys [part-of-semy?
                                part-of-charge-group?
                                ornament?]}]
  (let [type (-> charge :type name keyword)]
    (-> (cond
          (= type :escutcheon) (options/pick default-options
                                             [[:type]
                                              [:origin]
                                              [:anchor]
                                              [:geometry]
                                              [:escutcheon]
                                              [:fimbriation]
                                              [:outline-mode]
                                              [:vertical-mask]
                                              [:ignore-layer-separator?]]
                                             {[:geometry :size :default] 30})
          (#{:roundel
             :annulet
             :billet
             :lozenge
             :fusil
             :mascle
             :rustre} type) (options/pick default-options
                                          [[:type]
                                           [:origin]
                                           [:anchor]
                                           [:geometry]
                                           [:fimbriation]
                                           [:outline-mode]
                                           [:vertical-mask]
                                           [:ignore-layer-separator?]]
                                          {[:geometry :reversed?] nil
                                           [:geometry :mirrored?] nil})
          (= type :crescent) (options/pick default-options
                                           [[:type]
                                            [:origin]
                                            [:anchor]
                                            [:geometry]
                                            [:fimbriation]
                                            [:outline-mode]
                                            [:vertical-mask]
                                            [:ignore-layer-separator?]]
                                           {[:geometry :mirrored?] nil})
          :else (options/pick default-options
                              [[:type]
                               [:origin]
                               [:anchor]
                               [:geometry]
                               [:fimbriation]
                               [:tincture]
                               [:outline-mode]
                               [:vertical-mask]
                               [:ignore-layer-separator?]]))
        (assoc :manual-blazon (:manual-blazon default-options))
        (cond->
          (or part-of-semy?
              part-of-charge-group?) (dissoc :origin))
        (update :origin (fn [position]
                          (when position
                            (position/adjust-options position (-> charge :origin)))))
        (update :anchor (fn [position]
                          (when position
                            (position/adjust-options position (-> charge :anchor)))))
        (update-in [:geometry :size] (fn [size]
                                       (when size
                                         (cond-> size
                                           ornament? (assoc :min 5
                                                            :max 400
                                                            :default 100)))))
        (update :fimbriation (fn [fimbriation]
                               (when fimbriation
                                 (-> (fimbriation/options (:fimbriation charge)
                                                          :base-options (:fimbriation default-options))
                                     (assoc :ui {:label strings/fimbriation
                                                 :form-type :fimbriation}))))))))

(defmethod interface/component-options :heraldry.component/charge [path data]
  (options data :ornament? (some #(= % :ornaments) path)))

(defn title [path context]
  (let [charge-type (interface/get-raw-data (conj path :type) context)
        attitude (or (interface/get-raw-data (conj path :attitude) context)
                     :none)
        facing (or (interface/get-raw-data (conj path :facing) context)
                   :none)
        charge-name (or (choice-map charge-type)
                        (util/translate-cap-first charge-type))]
    (util/combine " " [charge-name
                       (when-not (= attitude :none)
                         (util/translate attitude))
                       (when-not (#{:none :to-dexter} facing)
                         (util/translate facing))])))
