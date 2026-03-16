##################################################################
## Python interface for reading HIPO files.                     ##
## This interface is using fusion class from the hipo4 module   ##
## with "C" extern intraface defined in wrapper.cpp class.      ##
## the fusion interface allows opening multiple files through   ##
## providing unique handles for each opened file.               ##
##                                                              ##
## Author: G.Gavalian (2022)                                    ##
## Jefferson Lab --------------------------------               ##
##################################################################

import ctypes

class hreader:

    def __init__(self,libfilename):
        """ hipo reader python class interface to hipo fusion wrapper
            (use it wisely)
        """
        self.libPath    = libfilename
        self.hipo4lib   = ctypes.CDLL(self.libPath+'/libhipo4.so')
        self.status     = ctypes.c_int(0)
        self.handle     = -1
        
    def open(self,filename):
        """ open the file and save the handle returned by fusion wrapper.
            each opened file has unique handle, so many files can be opened
            in parallel.
        """
        self.inputFile = filename
        self.handle    = self.hipo4lib.fusion_open(ctypes.c_char_p(self.inputFile.encode('ascii')))
        print('file open handle = ',self.handle)
        
    def open_with_tag(self,filename, tag):
        """ open the file and save the handle returned by fusion wrapper.
            each opened file has unique handle, so many files can be opened
            in parallel.
        """        
        self.inputFile = filename
        self.handle    = self.hipo4lib.fusion_open_with_tag(ctypes.c_char_p(self.inputFile.encode('ascii')), tag)
        print('file open handle = ',self.handle)

    def define(self,bankname):
        """ define is used to declare a bank that will be read by the fusion wrapper
            each time next() is called. The banks are stored in the internal map for
            each opened file handle.
        """
        self.hipo4lib.fusion_define(self.handle, ctypes.c_char_p(bankname.encode('ascii')))

    def describe(self,bankname):
        """ define is used to declare a bank that will be read by the fusion wrapper
        each time next() is called. The banks are stored in the internal map for
        each opened file handle.
        """
        self.hipo4lib.fusion_describe(self.handle,ctypes.c_char_p(bankname.encode('ascii')))
        
    def getSize(self,bankname):
        """ returns size of the bank that was read for current event.
        """
        size = self.hipo4lib.fusion_bankSize(self.handle,ctypes.c_char_p(bankname.encode('ascii')))
        return size

    def next(self):
        """ reads next event in the file, and reads all the banks that were decalred with
            hreader.define(bankname) function call.
        """
        status = self.hipo4lib.fusion_next(self.handle)
        return status==1

    def getInt(self,bank,entry,row):
        """ returns an integer value for entry and row from requested bank. call getSize()
            first to make sure that the row is within the allowable range to avoid hard crashes.
        """
        a1 = ctypes.c_char_p(bank.encode('ascii'))
        a2 = ctypes.c_char_p(entry.encode('ascii'))
        self.hipo4lib.fusion_get_float.restype = ctypes.c_int
        value = self.hipo4lib.fusion_get_int(self.handle,a1,a2,row)
        return value

    def getLong(self,bank,entry,row):
        """ returns an integer value for entry and row from requested bank. call getSize()
            first to make sure that the row is within the allowable range to avoid hard crashes.
        """
        a1 = ctypes.c_char_p(bank.encode('ascii'))
        a2 = ctypes.c_char_p(entry.encode('ascii'))
        self.hipo4lib.fusion_get_long.restype = ctypes.c_ulonglong
        value = self.hipo4lib.fusion_get_long(self.handle,a1,a2,row)
        return value
    
    def getFloat(self,bank,entry,row):
        """ returns an float value for entry and row from requested bank. call getSize()
        """
        a1 = ctypes.c_char_p(bank.encode('ascii'))
        a2 = ctypes.c_char_p(entry.encode('ascii'))
        self.hipo4lib.fusion_get_float.restype = ctypes.c_float
        value = ctypes.c_float(self.hipo4lib.fusion_get_float(self.handle,a1,a2,row)).value
        return value

    def getDouble(self,bank,entry,row):
        """ returns an float value for entry and row from requested bank. call getSize()
        """
        a1 = ctypes.c_char_p(bank.encode('ascii'))
        a2 = ctypes.c_char_p(entry.encode('ascii'))
        self.hipo4lib.fusion_get_float.restype = ctypes.c_double
        value = ctypes.c_double(self.hipo4lib.fusion_get_float(self.handle,a1,a2,row)).value
        return value

    def getType(self,bank,entry):
        """ returns type of the entry for the given bank. this is used to determine weather to
            use getInt or getFloat function.
        """
        a1 = ctypes.c_char_p(bank.encode('ascii'))
        a2 = ctypes.c_char_p(entry.encode('ascii'))
        type = self.hipo4lib.fusion_entry_type(self.handle,a1,a2)
        return type

    def getEntry(self,bank,entry):
        """ returns a python array containing all rows for given column. the determination of the
            is done internaly.
        """
        rows = self.getSize(bank)
        type = self.getType(bank,entry)
        array = []
        if(type==1 or type==2 or type==3):
            for row in range(rows):
                array.append(self.getInt(bank,entry,row))
        if(type==4):
            for row in range(rows):
                array.append(self.getFloat(bank,entry,row))        
        if(type==5):
            for row in range(rows):
                array.append(self.getDouble(bank,entry,row))
        if(type==8):
            for row in range(rows):
                array.append(self.getLong(bank,entry,row))

        return array
    
    def get_entries(self):
        """Returns the total number of entries (events) in the currently opened file."""
        self.hipo4lib.fusion_get_entries.restype = ctypes.c_int
        return self.hipo4lib.fusion_get_entries(self.handle)
    
    def show(self, bank: str):
        self.hipo4lib.fusion_show.restype = ctypes.c_voidp
        self.hipo4lib.fusion_show(self.handle, ctypes.c_char_p(bank.encode('ascii')))