(ns spec.heraldry.coat-of-arms-test
  (:require
   [cljs.spec.alpha :as s]
   [cljs.test :refer-macros [are deftest]]
   [spec.heraldry.specs]))

(defn check-spec [spec form]
  (let [explain-output (with-out-str (s/explain spec form))
        conforms? (s/valid? spec form)]
    (when-not conforms?
      (println explain-output))
    conforms?))

;; (deftest valid-ordinaries
;;   (are [spec form] (check-spec spec form)

;;     :heraldry/ordinary {:type :heraldry.ordinary.type/pale
;;                         :field {:type :heraldry.field.type/plain
;;                                 :tincture :azure}}

;;     :heraldry/ordinary {:type :heraldry.ordinary.type/fess
;;                         :field {:type :heraldry.field.type/plain
;;                                 :tincture :azure}
;;                         :geometry {:size 50}
;;                         :anchor {:point :fess
;;                                  :offset-x -5
;;                                  :offset-y 5}
;;                         :line {:type :engrailed
;;                                :eccentricity 1.3
;;                                :width 2
;;                                :offset 0.2
;;                                :flipped? false}
;;                         :opposite-line {:type :engrailed
;;                                         :eccentricity 1.3
;;                                         :width 2
;;                                         :offset 0.2
;;                                         :flipped? true}}))

;; (deftest invalid-ordinaries
;;   (are [spec form] (not (s/valid? spec form))

;;     :heraldry/ordinary {}

;;     :heraldry/ordinary {:type :heraldry.field.type/per-pale
;;                         :field {:type :heraldry.field.type/plain
;;                                 :tincture :azure}}

;;     :heraldry/ordinary {:type :does-not-exist
;;                         :field {:type :heraldry.field.type/plain
;;                                 :tincture :azure}}

;;     :heraldry/ordinary {:type :fess
;;                         :field {}}))

;; (deftest valid-coat-of-arms
;;   (are [spec form] (check-spec spec form)

;;     :heraldry/coat-of-arms {:spec-version 1
;;                             :type :coat-of-arms
;;                             :field {:type :heraldry.field.type/plain
;;                                     :tincture :azure}}

;;     :heraldry/coat-of-arms {:spec-version 1
;;                             :type :coat-of-arms
;;                             :field {:type :per-pale
;;                                     :line {:type :invected
;;                                            :eccentricity 1.3
;;                                            :width 2
;;                                            :offset 0.2
;;                                            :flipped? false}
;;                                     :fields [{:type :heraldry.field.type/plain
;;                                               :tincture :azure}
;;                                              {:type :heraldry.field.type/plain
;;                                               :tincture :or}]
;;                                     :outline? true}}))

;; (deftest invalid-coat-of-arms
;;   (are [spec form] (not (s/valid? spec form))

;;     :heraldry/coat-of-arms {}

;;     :heraldry/coat-of-arms {:spec-version 1}

;;     :heraldry/coat-of-arms {:field {:type :heraldry.field.type/plain
;;                                     :tincture :azure}}))
