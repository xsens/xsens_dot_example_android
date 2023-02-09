package com.xsens.dot.android.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xsens.dot.android.example.R


/**
 * A simple [Fragment] subclass.
 * Use the [ExportFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExportFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_export, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ExportFragment.
         */
        @JvmStatic
        fun newInstance() =
            ExportFragment().apply {
                arguments = Bundle().apply {}
            }
    }
}