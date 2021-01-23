(ns heraldry.spec.test
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer-macros [deftest is]]
            [heraldry.spec.core]))

(deftest spec-field-content
  (let [form {:component :field
              :content {:tincture :azure}}]
    (s/explain :heraldry/field form)
    (is (s/valid? :heraldry/field form))))

(deftest spec-field-division
  (let [form {:component :field
              :division {:type :per-pale
                         :line {:type :invected}
                         :fields [{:component :field
                                   :content {:tincture :azure}}
                                  {:component :field
                                   :content {:tincture :or}}]
                         :hints {:outline? true}}}]
    (s/explain :heraldry/field form)
    (is (s/valid? :heraldry/field form))))

(deftest spec-field-invalid-no-content
  (let [form {:component :field}]
    (is (not (s/valid? :heraldry/field form)))))
