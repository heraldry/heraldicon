(ns heraldicon.context-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [heraldicon.context :as c]))

(def ^:private root
  {:path [:arms :coat-of-arms]
   :render-options-path [:arms :render-options]})

(deftest stepping-down-and-up
  (testing "same results as plain path manipulation"
    (is (= (:path (c/++ root :field :components 0)) [:arms :coat-of-arms :field :components 0]))
    (is (= (:path (c/-- (c/++ root :field :components 0) 2)) [:arms :coat-of-arms :field]))
    (is (= (:path (c/-- root 5)) []))
    (is (= (:path (c/++ {} :a)) [:a]))
    (is (= (:path (c/++ {:path '(:a :b)} :c)) [:a :b :c])))

  (testing "the same step returns the identical context"
    (is (identical? (c/++ root :field) (c/++ root :field)))
    (is (identical? (c/++ root :field :components 0) (c/++ (c/++ root :field) :components 0))))

  (testing "stepping back up returns the identical parent"
    (is (identical? (c/-- (c/++ root :field)) root))
    (is (identical? (c/-- (c/++ root :field :components 0) 3) root)))

  (testing "a context changed after the step falls back to the path"
    (let [changed (c/<< (c/++ root :field) :size-default 50)
          up (c/-- changed)]
      (is (= (:path up) (:path root)))
      (is (= (:size-default up) 50)))))

(deftest scrubbing-render-hints
  (let [hinted (c/set-render-hint root :svg-export? true)]
    (is (identical? (c/scrub-render-hints root) root))
    (is (= (c/scrub-render-hints hinted) root))
    (is (identical? (c/scrub-render-hints hinted) (c/scrub-render-hints hinted)))))
